import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.table.AbstractTableModel;

import com.skellix.database.table.ColumnType;

public class TableConfigModel extends AbstractTableModel {

	List<Object[]> rowData = new ArrayList<>();
	{
		rowData.add(createBasicRow());
	}
	String[] columnNames = new String[]{"Type", "Name", "Size", "Remove Column"};
	private Consumer<TableConfigModel> changeListener;
	
	public Object[] createBasicRow() {
		
		return new Object[]{ColumnType.INT, "key", "" + ColumnType.INT.defaultByteLength(), "Remove Column"};
	}

	@Override
	public String getColumnName(int column) {
		
		return columnNames[column];
	}

	@Override
	public int getRowCount() {
		return rowData.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		
		if (columnIndex == 0) {
			
			return ColumnType.class;
			
		}
		return String.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		return rowData.get(rowIndex)[columnIndex];
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		
		rowData.get(rowIndex)[columnIndex] = aValue;
		
		if (columnIndex == 0) {
			
			if (aValue instanceof ColumnType) {
				ColumnType columnType = (ColumnType) aValue;
				rowData.get(rowIndex)[2] = Integer.toString(columnType.defaultByteLength());
			}
		}
		
		if (changeListener != null) {
			
			changeListener.accept(this);
		}
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		
		return true;
	}

	public void addRow(Object[] row) {
		rowData.add(row);
		
		if (changeListener != null) {
			
			changeListener.accept(this);
		}
	}
	
	public void removeRow(Object[] row) {
		rowData.remove(row);
		
		if (changeListener != null) {
			
			changeListener.accept(this);
		}
	}

	public void removeRow(int row) {
		rowData.remove(row);
		
		if (changeListener != null) {
			
			changeListener.accept(this);
		}
	}

	public void addChangeListener(Consumer<TableConfigModel> changeListener) {
		
		this.changeListener = changeListener;
	}
}
