package x.mvmn.jscrcap.model;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class CapturesTableModel implements TableModel, Serializable {

	private static final long serialVersionUID = -1641800118624192876L;
	private final CopyOnWriteArrayList<CapturedImage> capturedImages = new CopyOnWriteArrayList<CapturedImage>();
	private final ConcurrentLinkedQueue<TableModelListener> modelListeners = new ConcurrentLinkedQueue<TableModelListener>();

	@Override
	public int getRowCount() {
		return capturedImages.size();
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return "Captures";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return CapturedImage.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public CapturedImage getValueAt(int rowIndex, int columnIndex) {
		return capturedImages.get(rowIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		modelListeners.add(l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		modelListeners.remove(l);
	}

	public void delete(int index) {
		if (index < capturedImages.size()) {
			capturedImages.remove(index);
			TableModelEvent changeEvent = new TableModelEvent(this, index, index, 0, TableModelEvent.DELETE);
			fireEvent(changeEvent);
		}
	}

	protected void fireEvent(TableModelEvent event) {
		for (TableModelListener listener : modelListeners) {
			try {
				listener.tableChanged(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addImage(CapturedImage image) {
		capturedImages.add(image);
		int modifiedRow = capturedImages.size();
		TableModelEvent changeEvent = new TableModelEvent(this, modifiedRow, modifiedRow, 0, TableModelEvent.INSERT);
		fireEvent(changeEvent);
	}

}
