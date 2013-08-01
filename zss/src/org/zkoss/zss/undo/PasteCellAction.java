/* PasteCellAction.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/7/25, Created by Dennis.Chen
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package org.zkoss.zss.undo;

import org.zkoss.zss.api.CellOperationUtil;
import org.zkoss.zss.api.Range;
import org.zkoss.zss.api.Range.PasteOperation;
import org.zkoss.zss.api.Range.PasteType;
import org.zkoss.zss.api.Ranges;
import org.zkoss.zss.api.model.Sheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.undo.impl.AbstractCellDataStyleAction;
import org.zkoss.zss.undo.impl.ReserveUtil;

/**
 * This undoable action doesn't handle merge cell when undo redo
 * @author dennis
 * 
 */
public class PasteCellAction extends AbstractCellDataStyleAction {

	protected final int _destRow,_destColumn,_destLastRow,_destLastColumn;
	protected final int _reservedDestLastRow,_reservedDestLastColumn;
	protected final Sheet _destSheet;
	protected final PasteType _pasteType;
	protected final PasteOperation _pasteOperation;
	protected final boolean _skipBlank;
	protected final boolean _transpose;
	
	private Range _pastedRange;
	
//	private final int rlastRow;
//	private final int rlastColumn;
	
	public PasteCellAction(String label, 
			Sheet sheet, int srcRow, int srcColumn,int srcLastRow, int srcLastColumn, 
			Sheet destSheet, int destRow, int destColumn,int destLastRow, int destLastColumn, 
			PasteType pasteType, PasteOperation pasteOperation, boolean skipBlank, boolean transpose) {
		super(label, sheet, srcRow, srcColumn, srcLastRow, srcLastColumn,RESERVE_ALL);
		this._transpose = transpose;
		
		this._destRow = destRow;
		this._destColumn = destColumn;
		this._destLastRow = destLastRow;
		this._destLastColumn = destLastColumn;
		
		int srcColNum = srcLastColumn-srcColumn;
		int srcRowNum = srcLastRow-srcRow;
		//enlarge and transpose
		int destWidth = Math.max(destLastColumn-destColumn, transpose?srcRowNum:srcColNum);
		int destHeight = Math.max(destLastRow-destRow, transpose?srcColNum:srcRowNum);
		
		_reservedDestLastRow = _destRow + destHeight;
		_reservedDestLastColumn = _destColumn + destWidth;
		
		this._destSheet = destSheet;
		this._pasteType = pasteType;
		this._pasteOperation = pasteOperation;
		this._skipBlank = skipBlank;
		
	}

	@Override
	protected int getReservedRow(){
		return _destRow;
	}
	@Override
	protected int getReservedColumn(){
		return _destColumn;
	}
	@Override
	protected int getReservedLastRow(){
		return _reservedDestLastRow;
	}
	@Override
	protected int getReservedLastColumn(){
		return _reservedDestLastColumn;
	}
	protected Sheet getReservedSheet(){
		return _destSheet;
	}
	@Override
	public Rect getUndoSelection(){
		return _pastedRange==null?new Rect(_destColumn,_destRow,_destLastColumn,_destLastRow):
			new Rect(_pastedRange.getColumn(),_pastedRange.getRow(),_pastedRange.getLastColumn(),_pastedRange.getLastRow());
	}
	@Override
	public Rect getRedoSelection(){
		return _pastedRange==null?new Rect(_destColumn,_destRow,_destLastColumn,_destLastRow):
			new Rect(_pastedRange.getColumn(),_pastedRange.getRow(),_pastedRange.getLastColumn(),_pastedRange.getLastRow());
	}
	
	protected void applyAction() {
		Range src = Ranges.range(_sheet, _row, _column, _lastRow, _lastColumn);
		Range dest = Ranges.range(_destSheet, _destRow, _destColumn, _destLastRow, _destLastColumn);
		_pastedRange = CellOperationUtil.pasteSpecial(src, dest, _pasteType, _pasteOperation, _skipBlank, _transpose);
	}

}