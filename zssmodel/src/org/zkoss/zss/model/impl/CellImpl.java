/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.model.impl;

import java.io.Serializable;
import java.util.Date;

import org.zkoss.zss.model.CellRegion;
import org.zkoss.zss.model.ErrorValue;
import org.zkoss.zss.model.InvalidateFormulaException;
import org.zkoss.zss.model.InvalidateModelOpException;
import org.zkoss.zss.model.SBookSeries;
import org.zkoss.zss.model.SCellStyle;
import org.zkoss.zss.model.SCellValue;
import org.zkoss.zss.model.SColumnArray;
import org.zkoss.zss.model.SComment;
import org.zkoss.zss.model.SHyperlink;
import org.zkoss.zss.model.SRichText;
import org.zkoss.zss.model.SSheet;
import org.zkoss.zss.model.SCell.CellType;
import org.zkoss.zss.model.sys.EngineFactory;
import org.zkoss.zss.model.sys.dependency.Ref;
import org.zkoss.zss.model.sys.formula.FormulaClearContext;
import org.zkoss.zss.model.sys.formula.FormulaEngine;
import org.zkoss.zss.model.sys.formula.FormulaEvaluationContext;
import org.zkoss.zss.model.sys.formula.FormulaExpression;
import org.zkoss.zss.model.sys.formula.FormulaParseContext;
import org.zkoss.zss.model.util.Validations;
//import org.zkoss.zss.ngmodel.InvalidateModelValueException;
/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public class CellImpl extends AbstractCellAdv {
	private static final long serialVersionUID = 1L;
	private AbstractRowAdv row;
	private int index;
	private SCellValue localValue = null;
	private AbstractCellStyleAdv cellStyle;
	transient private FormulaResultCellValue formulaResultValue;// cache
	
	
	//use another object to reduce object reference size
	private OptFields opts;
	
	private static class OptFields implements Serializable{
		private AbstractHyperlinkAdv hyperlink;
		private AbstractCommentAdv comment;
	}

	private OptFields getOpts(boolean create){
		if(opts==null && create){
			opts = new OptFields();
		}
		return opts;
	}

	public CellImpl(AbstractRowAdv row, int index) {
		this.row = row;
		this.index = index;
	}

	@Override
	public CellType getType() {
		SCellValue val = getCellValue();
		return val==null?CellType.BLANK:val.getType();
	}

	@Override
	public boolean isNull() {
		return false;
	}

	@Override
	public int getRowIndex() {
		checkOrphan();
		return row.getIndex();
	}

	@Override
	public int getColumnIndex() {
		checkOrphan();
		return index;
	}

	@Override
	public String getReferenceString() {
		return new CellRegion(getRowIndex(), getColumnIndex()).getReferenceString();
	}

	@Override
	public void checkOrphan() {
		if (row == null) {
			throw new IllegalStateException("doesn't connect to parent");
		}
	}

	@Override
	public SSheet getSheet() {
		checkOrphan();
		return row.getSheet();
	}

	@Override
	public void destroy() {
		checkOrphan();
		clearValue();
		row = null;
	}

	@Override
	public SCellStyle getCellStyle() {
		return getCellStyle(false);
	}

	@Override
	public SCellStyle getCellStyle(boolean local) {
		if (local || cellStyle != null) {
			return cellStyle;
		}
		checkOrphan();
		cellStyle = (AbstractCellStyleAdv) row.getCellStyle(true);
		AbstractSheetAdv sheet = (AbstractSheetAdv)row.getSheet();
		if (cellStyle == null) {
			SColumnArray array = sheet.getColumnArray(getColumnIndex());
			if(array!=null){
				cellStyle = (AbstractCellStyleAdv)((AbstractColumnArrayAdv)array).getCellStyle(true);
			}
		}
		if (cellStyle == null) {
			cellStyle = (AbstractCellStyleAdv) sheet.getBook()
					.getDefaultCellStyle();
		}
		return cellStyle;
	}

	@Override
	public void setCellStyle(SCellStyle cellStyle) {
		if(cellStyle!=null){
			Validations.argInstance(cellStyle, AbstractCellStyleAdv.class);
		}
		this.cellStyle = (AbstractCellStyleAdv) cellStyle;
		addCellUpdate();
	}

	@Override
	protected void evalFormula() {
		if (formulaResultValue == null) {
			SCellValue val = getCellValue();
			if(val!=null &&  val.getType() == CellType.FORMULA){
				FormulaEngine fe = EngineFactory.getInstance()
						.createFormulaEngine();
				formulaResultValue = new FormulaResultCellValue(fe.evaluate((FormulaExpression) val.getValue(),
						new FormulaEvaluationContext(this)));
			}
		}
	}

	@Override
	public CellType getFormulaResultType() {
		checkType(CellType.FORMULA);
		evalFormula();

		return formulaResultValue.getCellType();
	}

	@Override
	public void clearValue() {
		checkOrphan();
		clearFormulaDependency();
		clearFormulaResultCache();
		
		setCellValue(null);
		
		OptFields opts = getOpts(false); 
		if(opts!=null){
			// clear for value, don't clear hyperlink
//			opts.hyperlink = null;
		};
		//don't update when sheet is destroying
		if(BookImpl.destroyingSheet.get()!=getSheet()){
			addCellUpdate();
		}
	}
	
	private void addCellUpdate(){
		ModelUpdateUtil.addCellUpdate(getSheet(), getRowIndex(), getColumnIndex());
	}
	
	@Override
	public void setFormulaValue(String formula) {
		checkOrphan();
		Validations.argNotNull(formula);
		FormulaEngine fe = EngineFactory.getInstance().createFormulaEngine();
		FormulaExpression expr = fe.parse(formula, new FormulaParseContext(this,null));//for test error, no need to build dependency
		if(expr.hasError()){	
			String msg = expr.getErrorMessage();
			throw new InvalidateFormulaException(msg==null?"The formula ="+formula+" contains error":msg);
		}
		
		if(getType()==CellType.FORMULA){
			clearValueForSet(true);
		}
		
		//parse again, this will create new dependency
		expr = fe.parse(formula, new FormulaParseContext(this ,getRef()));
		setValue(expr);
	}
	
	private void clearValueForSet(boolean clearDependency) {
		//in some situation, we should clear dependency (e.g. old type and new type are both formula)
		if(clearDependency){
			clearFormulaDependency();
		}
		clearFormulaResultCache();
		
		OptFields opts = getOpts(false); 
		if(opts!=null){
			// Clear value only, don't clear hyperlink
//			opts.hyperlink = null;
		};
	}

	@Override
	public void clearFormulaResultCache() {
		if(formulaResultValue!=null){			
			//only clear when there is a formula result, or poi will do full cache scan to clean blank.
			EngineFactory.getInstance().createFormulaEngine().clearCache(new FormulaClearContext(this));
		}
		
		formulaResultValue = null;
	}
	
	@Override
	public boolean isFormulaParsingError() {
		if (getType() == CellType.FORMULA) {
			return ((FormulaExpression)getValue(false)).hasError();
		}
		return false;
	}
	
	private void clearFormulaDependency(){
		if(getType()==CellType.FORMULA){
			((AbstractBookSeriesAdv) getSheet().getBook().getBookSeries())
					.getDependencyTable().clearDependents(getRef());
		}
	}

	@Override
	public Object getValue(boolean evaluatedVal) {
		SCellValue val = getCellValue();
		if (evaluatedVal && val!=null && val.getType() == CellType.FORMULA) {
			evalFormula();
			return this.formulaResultValue.getValue();
		}
		return val==null?null:val.getValue();
	}

	private boolean isFormula(String string) {
		return string != null && string.startsWith("=") && string.length() > 1;
	}
	
	private SCellValue getCellValue(){
		checkOrphan();
		return localValue;
	}
	
	private void setCellValue(SCellValue value){
		checkOrphan();
		this.localValue = value!=null&&value.getType()==CellType.BLANK?null:value;
		
		//clear the dependent's formula result cache
		SBookSeries bookSeries = getSheet().getBook().getBookSeries();
		ModelUpdateUtil.handlePrecedentUpdate(bookSeries,getRef());
	}

	
	private static boolean valueEuqals(Object val1, Object val2){
		return val1==val2||(val1!=null && val1.equals(val2));
	}
	
	@Override
	public void setValue(Object newVal) {
		SCellValue oldVal = getCellValue();
		if( (oldVal==null && newVal==null) ||
			(oldVal != null && valueEuqals(oldVal.getValue(),newVal))) {
			return;
		}
		
		CellType newType;
		
		if (newVal == null) {
			newType = CellType.BLANK;
		} else if (newVal instanceof String) {
			if (isFormula((String) newVal)) {
				setFormulaValue(((String) newVal).substring(1));
				return;// break;
			} else {
				newType = CellType.STRING;
			}
		} else if (newVal instanceof SRichText) {
			newType = CellType.STRING;
		} else if (newVal instanceof FormulaExpression) {
			newType = CellType.FORMULA;
		} else if (newVal instanceof Date) {
			newType = CellType.NUMBER;
			newVal = EngineFactory.getInstance().getCalendarUtil().dateToDoubleValue((Date)newVal);
		} else if (newVal instanceof Boolean) {
			newType = CellType.BOOLEAN;
		} else if (newVal instanceof Double) {
			newType = CellType.NUMBER;
		} else if (newVal instanceof Number) {
			newType = CellType.NUMBER;
			newVal = ((Number)newVal).doubleValue();
		} else if (newVal instanceof ErrorValue) {
			newType = CellType.ERROR;
		} else {
			throw new IllegalArgumentException(
					"unsupported type "
							+ newVal
							+ ", supports NULL, String, Date, Number and Byte(as Error Code)");
		}

		
		SCellValue newCellVal = new InnerCellValue(newType,newVal);
		//should't clear dependency if new type is formula, it clear the dependency already when eval
		clearValueForSet(oldVal!=null && oldVal.getType()==CellType.FORMULA && newType !=CellType.FORMULA);
		
		setCellValue(newCellVal);
		addCellUpdate();
	}

	

	@Override
	public SHyperlink getHyperlink() {
		OptFields opts = getOpts(false);
		return opts==null?null:opts.hyperlink;
	}

	@Override
	public void setHyperlink(SHyperlink hyperlink) {
		Validations.argInstance(hyperlink, AbstractHyperlinkAdv.class);
		getOpts(true).hyperlink = (AbstractHyperlinkAdv)hyperlink;
		addCellUpdate();
	}
	
	@Override
	public SComment getComment() {
		OptFields opts = getOpts(false);
		return opts==null?null:opts.comment;
	}

	@Override
	public void setComment(SComment comment) {
		Validations.argInstance(comment, AbstractCommentAdv.class);
		getOpts(true).comment = (AbstractCommentAdv)comment;
		addCellUpdate();
	}
	
	@Override
	void setIndex(int newidx) {
		if(this.index==newidx){
			return;
		}
		
		CellType type = getType();
		String formula = null;
		if(type == CellType.FORMULA){
			formula = getFormulaValue();
			((AbstractBookSeriesAdv) getSheet().getBook().getBookSeries())
				.getDependencyTable().clearDependents(getRef());
		}
		this.index = newidx;
		if(formula!=null){
			FormulaEngine fe = EngineFactory.getInstance().createFormulaEngine();
			fe.parse(formula, new FormulaParseContext(this ,getRef()));//rebuild the expression to make new dependency with current row,column
		}
	}
	
	@Override
	void setRow(int oldRowIdx, AbstractRowAdv row){
		if(oldRowIdx==row.getIndex() && this.row==row){
			return;
		}
		
		CellType type = getType();
		String formula = null;
		if(type == CellType.FORMULA){
			formula = getFormulaValue();
			//clear the old dependency
			SSheet sheet = getSheet();
			Ref oldRef = new RefImpl(sheet.getBook().getBookName(),sheet.getSheetName(),oldRowIdx,getColumnIndex());
			((AbstractBookSeriesAdv) getSheet().getBook().getBookSeries()).getDependencyTable().clearDependents(oldRef);
		}
		this.row = row;
		if(formula!=null){
			FormulaEngine fe = EngineFactory.getInstance().createFormulaEngine();
			fe.parse(formula, new FormulaParseContext(this ,getRef()));//rebuild the expression to make new dependency with current row,column
		}
	}
	

	protected Ref getRef(){
		return new RefImpl(this);
	}
	
	private static class InnerCellValue extends SCellValue{
		private static final long serialVersionUID = 1L;
		private InnerCellValue(CellType type, Object value){
			super(type,value);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Cell:"+getReferenceString()+"[").append(getRowIndex()).append(",").append(getColumnIndex()).append("]");
		return sb.toString();
	}
}