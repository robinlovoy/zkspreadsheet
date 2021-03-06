/* TextIndentHandler.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2014/3/23 , Created by JerryChen
}}IS_NOTE

Copyright (C) 2015 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ui.impl.ua;

import org.zkoss.util.resource.Labels;
import org.zkoss.zss.api.CellOperationUtil;
import org.zkoss.zss.api.Range;
import org.zkoss.zss.api.Ranges;
import org.zkoss.zss.api.AreaRef;
import org.zkoss.zss.api.model.Sheet;
import org.zkoss.zss.api.model.CellStyle.Alignment;
import org.zkoss.zss.ui.CellSelectionType;
import org.zkoss.zss.ui.UserActionContext;
import org.zkoss.zss.ui.impl.undo.CellStyleAction;
import org.zkoss.zss.ui.sys.UndoableActionManager;

/**
 * @author JerryChen
 *
 */
public class TextIndentHandler extends AbstractCellHandler {
	private static final long serialVersionUID = -2346819389887059442L;
	private int offset;
	
	
	public TextIndentHandler(int offset) {
		this.offset = offset;
	}


	/* (non-Javadoc)
	 * @see org.zkoss.zss.ui.sys.ua.impl.AbstractHandler#processAction(org.zkoss.zss.ui.UserActionContext)
	 */
	@Override
	protected boolean processAction(UserActionContext ctx) {
		Sheet sheet = ctx.getSheet();
		AreaRef selection = ctx.getSelection();
		CellSelectionType type = ctx.getSelectionType();
		Range range = Ranges.range(sheet, selection);
		//ZSS-576
		if (range.isProtected() && !range.getSheetProtection().isFormatCellsAllowed()) {
			showProtectMessage();
			return true;
		}	
		//zss-623, extends to row,column area
		switch(type){
		case ROW:
			range = range.toRowRange();
			break;
		case COLUMN:
			range = range.toColumnRange();
			break;
		case ALL:
			//we don't allow to set whole sheet style, use column range instead 
			range = range.toColumnRange();
		}
		selection = new AreaRef(range.getRow(),range.getColumn(),range.getLastRow(),range.getLastColumn());
		UndoableActionManager uam = ctx.getSpreadsheet().getUndoableActionManager();
		uam.doAction(new CellStyleAction(Labels.getLabel("zss.undo.cellStyle"),sheet, selection.getRow(), selection.getColumn(), 
			selection.getLastRow(), selection.getLastColumn(), 
			CellOperationUtil.getIndentionApplier(offset)));
		return true;
	}

}
