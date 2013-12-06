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
package org.zkoss.zss.ngmodel.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.zkoss.zss.ngmodel.InvalidateModelOpException;
import org.zkoss.zss.ngmodel.ModelEvent;
import org.zkoss.zss.ngmodel.ModelEventListener;
import org.zkoss.zss.ngmodel.ModelEvents;
import org.zkoss.zss.ngmodel.NBookSeries;
import org.zkoss.zss.ngmodel.NCell;
import org.zkoss.zss.ngmodel.NCellStyle;
import org.zkoss.zss.ngmodel.NColor;
import org.zkoss.zss.ngmodel.NFont;
import org.zkoss.zss.ngmodel.NName;
import org.zkoss.zss.ngmodel.NSheet;
import org.zkoss.zss.ngmodel.util.CellStyleMatcher;
import org.zkoss.zss.ngmodel.util.FontMatcher;
import org.zkoss.zss.ngmodel.util.SpreadsheetVersion;
import org.zkoss.zss.ngmodel.util.Strings;
import org.zkoss.zss.ngmodel.util.Validations;

/**
 * @author dennis
 * @since 3.5.0
 */
public class BookImpl extends BookAdv{
	private static final long serialVersionUID = 1L;

	private final String bookName;
	
	private NBookSeries bookSeries;
	
	private final List<SheetAdv> sheets = new LinkedList<SheetAdv>();
	private final List<NameAdv> names = new LinkedList<NameAdv>();
	
	private final List<CellStyleAdv> cellStyles = new LinkedList<CellStyleAdv>();
	private final CellStyleAdv defaultCellStyle;
	private final List<FontAdv> fonts = new LinkedList<FontAdv>();
	private final FontAdv defaultFont;
	private final HashMap<ColorAdv,ColorAdv> colors = new LinkedHashMap<ColorAdv,ColorAdv>();
	

	
	private final HashMap<String,AtomicInteger> objIdCounter = new HashMap<String,AtomicInteger>();
	private final int maxRowSize = SpreadsheetVersion.EXCEL2007.getMaxRows();
	private final int maxColumnSize = SpreadsheetVersion.EXCEL2007.getMaxColumns();
	
	private final List<ModelEventListener> listeners = new LinkedList<ModelEventListener>();
	
	private final HashMap<String,Object> attributes = new LinkedHashMap<String, Object>();
	
	public BookImpl(String bookName){
		Validations.argNotNull(bookName);
		this.bookName = bookName;
		bookSeries = new BookSeriesImpl(this);
		fonts.add(defaultFont = new FontImpl());
		cellStyles.add(defaultCellStyle = new CellStyleImpl(defaultFont));
		colors.put(ColorImpl.WHITE,ColorImpl.WHITE);
		colors.put(ColorImpl.BLACK,ColorImpl.BLACK);
		colors.put(ColorImpl.RED,ColorImpl.RED);
		colors.put(ColorImpl.GREEN,ColorImpl.GREEN);
		colors.put(ColorImpl.BLUE,ColorImpl.BLUE);
		
	}
	
	@Override
	public NBookSeries getBookSeries(){
		return bookSeries;
	}
	
	@Override
	public String getBookName(){
		return bookName;
	}
	
	@Override
	public NSheet getSheet(int i){
		return sheets.get(i);
	}
	
	@Override
	public int getNumOfSheet(){
		return sheets.size();
	}
	
	@Override
	public NSheet getSheetByName(String name){
		for(NSheet sheet:sheets){
			if(sheet.getSheetName().equalsIgnoreCase(name)){
				return sheet;
			}
		}
		return null;
	}
	
	protected void checkOwnership(NSheet sheet){
		if(!sheets.contains(sheet)){
			throw new InvalidateModelOpException("doesn't has ownership "+ sheet);
		}
	}
	protected void checkOwnership(NName name){
		if(!names.contains(name)){
			throw new InvalidateModelOpException("doesn't has ownership "+ name);
		}
	}
	
//	protected String suggestSheetName(String basename){
//		int i = 1;
//		HashSet<String> names = new HashSet<String>();
//		for(NSheet sheet:sheets){
//			names.add(sheet.getSheetName());
//		}
//		String name = basename==null?"Sheet 1":basename;
//		while(names.contains(name)){
//			name = basename + " "+i++;
//		};
//		return name;
//	}
	
	@Override
	public void sendEvent(ModelEvent event){	
		//implicitly deliver to sheet
		for(SheetAdv sheet:sheets){
			sheet.onModelEvent(event);
		}
		
		for(ModelEventListener l:listeners){
			l.onEvent(event);
		}
	}
	
	@Override
	public void sendEvent(String name, Object... data){
		Map<String,Object> datamap = new HashMap<String,Object>();
		datamap.put("book", this);
		if(datamap!=null){
			if(data.length%2 != 0){
				throw new IllegalArgumentException("event data must be key,value pair");
			}
			for(int i=0;i<data.length;i+=2){
				if(!(data[i] instanceof String)){
					throw new IllegalArgumentException("event data key must be string");
				}
				datamap.put((String)data[i],data[i+1]);
			}
		}
		ModelEvent event = new ModelEvent(name, datamap);
		sendEvent(event);
	}
	
	@Override
	public NSheet createSheet(String name) {
		return createSheet(name,null);
	}
	
	@Override
	String nextObjId(String type){
		StringBuilder sb = new StringBuilder(type);
		sb.append("_");
		AtomicInteger i = objIdCounter.get(type);
		if(i==null){
			objIdCounter.put(type, i = new AtomicInteger(0));
		}
		sb.append(i.getAndIncrement());
		return sb.toString();
	}
	
	@Override
	public NSheet createSheet(String name,NSheet src) {
		checkLegalSheetName(name);
		if(src!=null)
			checkOwnership(src);
		

		SheetAdv sheet = new SheetImpl(this,nextObjId("sheet"));
		if(src instanceof SheetAdv){
			((SheetAdv)src).copyTo(sheet);
		}
		((SheetAdv)sheet).setSheetName(name);
		sheets.add(sheet);
		
		sendEvent(ModelEvents.ON_SHEET_ADDED, 
				ModelEvents.PARAM_SHEET, sheet);
		return sheet;
	}

	@Override
	public void setSheetName(NSheet sheet, String newname) {
		checkLegalSheetName(newname);
		checkOwnership(sheet);
		
		String oldname = sheet.getSheetName();
		((SheetAdv)sheet).setSheetName(newname);
		
		sendEvent(ModelEvents.ON_SHEET_RENAMED, 
				ModelEvents.PARAM_SHEET, sheet,
				ModelEvents.PARAM_SHEET_OLD_NAME, oldname);
	}

	private void checkLegalSheetName(String name) {
		if(Strings.isBlank(name)){
			throw new InvalidateModelOpException("sheet name '"+name+"' is not legal");
		}
		if(getSheetByName(name)!=null){
			throw new InvalidateModelOpException("sheet name '"+name+"' is dpulicated");
		}
		//TODO
	}
	
	private void checkLegalNameName(String name) {
		if(Strings.isBlank(name)){
			throw new InvalidateModelOpException("name '"+name+"' is not legal");
		}
		if(getNameByName(name)!=null){
			throw new InvalidateModelOpException("name '"+name+"' is dpulicated");
		}
		//TODO
	}

	@Override
	public void deleteSheet(NSheet sheet) {
		checkOwnership(sheet);
		
		((SheetAdv)sheet).destroy();
		
		int index = sheets.indexOf(sheet);
		sheets.remove(index);
		
		sendEvent(ModelEvents.ON_SHEET_DELETED, 
				ModelEvents.PARAM_SHEET, sheet,
				ModelEvents.PARAM_SHEET_OLD_INDEX, index);
	}

	@Override
	public void moveSheetTo(NSheet sheet, int index) {
		checkOwnership(sheet);
		if(index<0|| index>=sheets.size()){
			throw new InvalidateModelOpException("new position out of bound "+sheets.size() +"<>" +index);
		}
		int oldindex = sheets.indexOf(sheet);
		if(oldindex==index){
			return;
		}
		sheets.remove(oldindex);
		sheets.add(index, (SheetAdv)sheet);
		sendEvent(ModelEvents.ON_SHEET_MOVED, 
				ModelEvents.PARAM_SHEET, sheet,
				ModelEvents.PARAM_SHEET_OLD_INDEX, oldindex);
	}

	public void dump(StringBuilder builder) {
		for(SheetAdv sheet:sheets){
			if(sheet instanceof SheetImpl){
				((SheetImpl)sheet).dump(builder);
			}else{
				builder.append("\n").append(sheet);
			}
		}
	}

	@Override
	public NCellStyle getDefaultCellStyle() {
		return defaultCellStyle;
	}

	@Override
	public NCellStyle createCellStyle(boolean inStyleTable) {
		return createCellStyle(null,inStyleTable);
	}

	@Override
	public NCellStyle createCellStyle(NCellStyle src,boolean inStyleTable) {
		if(src!=null){
			Validations.argInstance(src, CellStyleAdv.class);
		}
		CellStyleAdv style = new CellStyleImpl(defaultFont);
		if(src!=null){
			((CellStyleAdv)src).copyTo(style);
		}
		
		if(inStyleTable){
			cellStyles.add(style);
		}
		
		return style;
	}
	
	@Override
	public NCellStyle searchCellStyle(CellStyleMatcher matcher) {
		for(NCellStyle style:cellStyles){
			if(matcher.match(style)){
				return style;
			}
		}
		return null;
	}
	
	
	@Override
	public NFont getDefaultFont() {
		return defaultFont;
	}

	@Override
	public NFont createFont(boolean inFontTable) {
		return createFont(null,inFontTable);
	}

	@Override
	public NFont createFont(NFont src,boolean inFontTable) {
		if(src!=null){
			Validations.argInstance(src, FontAdv.class);
		}
		FontAdv font = new FontImpl();
		if(src!=null){
			((FontAdv)src).copyTo(font);
		}
		
		if(inFontTable){
			fonts.add(font);
		}
		
		return font;
	}
	
	@Override
	public NFont searchFont(FontMatcher matcher) {
		for(NFont font:fonts){
			if(matcher.match(font)){
				return font;
			}
		}
		return null;
	}
	
	@Override
	public int getMaxRowSize() {
		return maxRowSize;
	}

	@Override
	public int getMaxColumnSize() {
		return maxColumnSize;
	}

	@Override
	List<NCell> optimizeCellStyle() {
		//search all the cell's style , 
		//if it is same as style in the style table (but different instance), then reassign the one in the table
		// 
		//if no one match a cell's style, then set it to style table.
		//(Optional) it total cell style are too many, search the similar cell style the get a similar style and reassign to the cell
		
		//TODO
		throw new UnsupportedOperationException("not implementate la.");
	}

	
	@Override
	public void addEventListener(ModelEventListener listener){
		if(!listeners.contains(listener)){
			listeners.add(listener);
		}
	}
	@Override
	public void removeEventListener(ModelEventListener listener){
		if(listeners!=null){
			listeners.remove(listener);
		}
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Object setAttribute(String name, Object value) {
		return attributes.put(name, value);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public NColor createColor(byte r, byte g, byte b) {
		ColorAdv newcolor = new ColorImpl(r,g,b);
		ColorAdv color = colors.get(newcolor);//reuse the existed color object
		if(color==null){
			colors.put(newcolor, color = newcolor);
		}
		return color;
	}

	@Override
	public NColor createColor(String htmlColor) {
		ColorAdv newcolor = new ColorImpl(htmlColor);
		ColorAdv color = colors.get(newcolor);//reuse the existed color object
		if(color==null){
			colors.put(newcolor, color = newcolor);
		}
		return color;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<NSheet> getSheets() {
		return Collections.unmodifiableList((List)sheets);
	}

	@Override
	public NName createName(String namename) {
		checkLegalNameName(namename);

		NameAdv name = new NameImpl(this,nextObjId("name"));
		name.setName(namename);
		names.add(name);
		
//		sendEvent(ModelEvents.ON_NAME_ADDED, 
//				ModelEvents.PARAM_SHEET, sheet);
		return name;
	}

	@Override
	public void setNameName(NName name, String newname) {
		checkLegalNameName(newname);
		checkOwnership(name);
		
		String oldname = name.getSheetName();
		((NameAdv)name).setName(newname);
		
//		sendEvent(ModelEvents.ON_NAME_RENAMED, 
//				ModelEvents.PARAM_SHEET, sheet,
//				ModelEvents.PARAM_SHEET_OLD_NAME, oldname);
	}

	@Override
	public void deleteName(NName name) {
		checkOwnership(name);
		
		((NameAdv)name).destroy();
		
		int index = names.indexOf(name);
		names.remove(index);
		
//		sendEvent(ModelEvents.ON_NAME_DELETED, 
//				ModelEvents.PARAM_NAME, sheet,
//				ModelEvents.PARAM_SHEET_OLD_INDEX, index);
	}

	@Override
	public int getNumOfName() {
		return names.size();
	}

	@Override
	public NName getName(int idx) {
		return names.get(idx);
	}

	@Override
	public NName getNameByName(String namename) {
		for(NName name:names){
			if(name.getName().equalsIgnoreCase(namename)){
				return name;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<NName> getNames() {
		return Collections.unmodifiableList((List)names);
	}

	@Override
	public int getSheetIndex(NSheet sheet) {
		return sheets.indexOf(sheet);
	}

}
