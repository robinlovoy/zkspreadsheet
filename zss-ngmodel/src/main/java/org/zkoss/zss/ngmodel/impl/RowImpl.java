package org.zkoss.zss.ngmodel.impl;

import org.zkoss.zss.ngmodel.ModelEvent;
import org.zkoss.zss.ngmodel.NCellStyle;
import org.zkoss.zss.ngmodel.NSheet;
import org.zkoss.zss.ngmodel.util.Validations;

public class RowImpl extends RowAdv {
	private static final long serialVersionUID = 1L;

	private SheetAdv sheet;

	private final BiIndexPool<CellAdv> cells = new BiIndexPool<CellAdv>();

	private CellStyleAdv cellStyle;

	public RowImpl(SheetAdv sheet) {
		this.sheet = sheet;
	}

	@Override
	public NSheet getSheet() {
		checkOrphan();
		return sheet;
	}

	@Override
	public int getIndex() {
		checkOrphan();
		return sheet.getRowIndex(this);
	}

	@Override
	public boolean isNull() {
		return false;
	}

	// public AbstractCell getCellAt(int columnIdx) {
	// return getCellAt(columnIdx, true);
	// }

	@Override
	CellAdv getCellAt(int columnIdx, boolean proxy) {
		CellAdv cellObj = cells.get(columnIdx);
		if (cellObj != null) {
			return cellObj;
		}
		checkOrphan();
		return proxy ? new CellProxy(sheet, getIndex(), columnIdx) : null;
	}

	@Override
	CellAdv getOrCreateCellAt(int columnIdx) {
		CellAdv cellObj = cells.get(columnIdx);
		if (cellObj == null) {
			checkOrphan();
			cellObj = new CellImpl(this);
			cells.put(columnIdx, cellObj);
			// create column since we have cell of it
			sheet.getOrCreateColumnAt(columnIdx);
		}
		return cellObj;
	}

	@Override
	int getCellIndex(CellAdv cell) {
		return cells.get(cell);
	}

	@Override
	public int getStartCellIndex() {
		return cells.firstKey();
	}

	@Override
	public int getEndCellIndex() {
		return cells.lastKey();
	}

	@Override
	protected void onModelEvent(ModelEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearCell(int start, int end) {
		// clear before move relation
		for (CellAdv cell : cells.subValues(start, end)) {
			cell.release();
		}
		cells.clear(start, end);
	}

	@Override
	public void insertCell(int cellIdx, int size) {
		if (size <= 0)
			return;

		cells.insert(cellIdx, size);
	}

	@Override
	public void deleteCell(int cellIdx, int size) {
		if (size <= 0)
			return;
		// clear before move relation
		for (CellAdv cell : cells.subValues(cellIdx, cellIdx + size)) {
			cell.release();
		}

		cells.delete(cellIdx, size);
	}

	@Override
	public String asString() {
		return String.valueOf(getIndex() + 1);
	}

	@Override
	public void checkOrphan() {
		if (sheet == null) {
			throw new IllegalStateException("doesn't connect to parent");
		}
	}

	@Override
	public void release() {
		checkOrphan();
		for (CellAdv cell : cells.values()) {
			cell.release();
		}
		sheet = null;
	}

	@Override
	public NCellStyle getCellStyle() {
		return getCellStyle(false);
	}

	@Override
	public NCellStyle getCellStyle(boolean local) {
		if (local || cellStyle != null) {
			return cellStyle;
		}
		checkOrphan();
		return sheet.getBook().getDefaultCellStyle();
	}

	@Override
	public void setCellStyle(NCellStyle cellStyle) {
		Validations.argNotNull(cellStyle);
		Validations.argInstance(cellStyle, CellStyleImpl.class);
		this.cellStyle = (CellStyleImpl) cellStyle;
	}

}
