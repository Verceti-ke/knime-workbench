/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 12, 2018 (hornm): created
 */
package org.knime.workbench.nodemonitorview;

import java.util.concurrent.ExecutionException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.tableview.AsyncDataRow;
import org.knime.core.node.tableview.AsyncTable;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.node.workflow.async.AsyncNodeOutPortUI;

/**
 * Puts (simple, i.e. as string) content of one output port table into the table.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class MonitorDataTable implements NodeMonitorTable {

    /**
     * Number of rows to 'pre-load' in view when data table is shown.
     */
    private static final int NUM_LOOK_AHEAD_ROWS = 42;

    /**
     * Limit the maximum number of columns when data table is shown to avoid a unresponsive UI.
     */
    private static final int MAX_NUM_COLUMN = 100;

    private RowIterator m_it;

    private long m_currentIndex = -1;

    private DataTable m_dataTable;

    // also used as indicator whether a table has been loaded completely
    private long m_numRows = -1;

    private long m_numLoadedRows = -1;

    private long m_numTotalRows = -1;

    private int m_numTotalCols = -1;

    private boolean m_autoLoad;

    /* --- in case of deactivated auto-load */

    private RowIterator m_chunkIt;

    private AddDataRowListener m_addDataRowListener;

    private final int m_portIndex;

    private Table m_table;

    /**
     * Creates a new monitor table to display node's data the given port index.
     *
     * @param portIndex port to use
     */
    public MonitorDataTable(final int portIndex) {
        m_portIndex = portIndex;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void loadTableData(final NodeContainerUI ncUI, final NodeContainer nc, final int count)
        throws LoadingFailedException {
        m_autoLoad = nc != null;
        if (count == 0) {
            // check if we can display something at all:
            int index = m_portIndex;
            if (ncUI instanceof SingleNodeContainerUI) {
                index++; // we don't care about (hidden) variable OutPort
            }
            if (ncUI.getNrOutPorts() <= index) {
                // no (real) port available
                throw new LoadingFailedException("No output ports");
            }
            if (!ncUI.getNodeContainerState().isExecuted()) {
                throw new LoadingFailedException("Node not executed");
            }
            NodeOutPortUI nop = ncUI.getOutPort(index);


            PortObject po;
            if (nop instanceof AsyncNodeOutPortUI) {
                try {
                    po = ((AsyncNodeOutPortUI)nop).getPortObjectAsync().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new LoadingFailedException("Problem retrieving the port object: " + e.getMessage());
                }
            } else {
                po = nop.getPortObject();
            }

            // retrieve table
            if (po instanceof BufferedDataTable) {
                BufferedDataTable table = (BufferedDataTable) po;
                m_numRows = m_numTotalRows = table.size();
                m_numTotalCols = table.getDataTableSpec().getNumColumns();
                m_dataTable = (DataTable)po;
            } else if (po instanceof KnowsRowCountTable) {
                KnowsRowCountTable table = (KnowsRowCountTable) po;
                m_numRows = m_numTotalRows = table.size();
                m_numTotalCols = table.getDataTableSpec().getNumColumns();
                m_dataTable = (KnowsRowCountTable)po;
            } else if (po instanceof DataTable && po instanceof AsyncTable) {
                assert !m_autoLoad;
                m_dataTable = (DataTable)po;
                m_numRows = m_numTotalRows = -1;
                m_numTotalCols = -1;
                if (po instanceof AsyncTable) {
                    ((AsyncTable)po).setRowCountKnownCallback(c -> {
                        m_numTotalRows = c;
                        m_numTotalCols = m_dataTable.getDataTableSpec().getNumColumns();
                    });
                }
            } else {
                // no table in port - ignore.
                throw new LoadingFailedException("Unknown or no PortObject");
            }
        }
        if (!m_autoLoad) {
            try {
                loadChunk(count);
            } catch (InterruptedException | ExecutionException e) {
                throw new LoadingFailedException("Loading of the table failed: " + e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupTable(final Table table) {
        m_table = table;
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(" ID ");
        for (int i = 0; i < Math.min(m_dataTable.getDataTableSpec().getNumColumns(), MAX_NUM_COLUMN - 2); i++) {
            column = new TableColumn(table, SWT.NONE);
            column.setText(m_dataTable.getDataTableSpec().getColumnSpec(i).getName());
        }
        if (table.getColumnCount() >= MAX_NUM_COLUMN - 1) {
            column = new TableColumn(table, SWT.NONE);
            column.setText("(remaining columns skipped)");
        }

        //needs to be added before calling 'setItemCount'
        //- first items will be empty otherwise
        m_addDataRowListener = new AddDataRowListener();
        table.addListener(SWT.SetData, m_addDataRowListener);

        int itemCountToDetermineColWidth;
        if (m_autoLoad && m_numRows >= 0) {
            int initialItemCount = Math.min((int)m_numRows, NUM_LOOK_AHEAD_ROWS);
            table.setItemCount(initialItemCount);
            itemCountToDetermineColWidth = initialItemCount;
        } else {
            table.setItemCount((int)m_numLoadedRows);
            itemCountToDetermineColWidth = 0;
        }
        for (int row = 0; row < itemCountToDetermineColWidth; row++) {
            fillTableItem(table.getItem(row));
        }
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn tableColumn = table.getColumn(i);
            tableColumn.pack();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateControls(final Button loadButton, final Combo portCombo, final int count) {
        if (count == 0) {
            loadButton.setText("Show rows");
        } else {
            loadButton.setText("Show more rows");
        }
        if (!m_autoLoad && m_numRows < 0) {
            loadButton.setEnabled(true);
        } else {
            loadButton.setEnabled(false);
        }
        portCombo.setEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInfoLabel(final Label info) {
        info.setText("Port Output");
    }

    @Override
    public void dispose(final Table table) {
        if (m_addDataRowListener != null) {
            table.removeListener(SWT.SetData, m_addDataRowListener);
        }
        closeIterator();
    }

    private void loadChunk(final int chunkIdx) throws InterruptedException, ExecutionException {
        if (m_chunkIt == null) {
            m_chunkIt = m_dataTable.iterator();
        }
        long startRowIdx = chunkIdx * NUM_LOOK_AHEAD_ROWS;
        long endRowIdx = (chunkIdx + 1) * NUM_LOOK_AHEAD_ROWS;

        long i;
        for (i = startRowIdx; i < endRowIdx && m_chunkIt.hasNext(); i++) {
            DataRow row = m_chunkIt.next();
            if (row instanceof AsyncDataRow) {
                ((AsyncDataRow)row).waitUntilLoaded();
            }
            if (row.getKey() == null) {
                // we reached the end of table
                m_numRows = m_numTotalRows = i;
                m_numTotalCols = row.getNumCells();
                break;
            }
        }
        m_numLoadedRows = i;
        if (!m_chunkIt.hasNext()) {
            m_numRows = m_numTotalRows = m_numLoadedRows;
            m_numTotalCols = m_dataTable.getDataTableSpec().getNumColumns();
        }
        if (m_table != null) {
            Display.getDefault().asyncExec(() -> m_table.setItemCount((int)m_numLoadedRows));
        }
    }

    private void closeIterator() {
        if (m_it instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_it).close();
        }
        if (m_chunkIt instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_chunkIt).close();
        }
    }

    /** Fills a table item (a row) with its data..
     * @param item Non-null item to be filled.
     */
    private void fillTableItem(final TableItem item) {
        int index = m_table.indexOf(item);
        int itemCount;
        if (m_autoLoad && m_numRows >= 0) {
            itemCount = Math.min((int)m_numRows, index + NUM_LOOK_AHEAD_ROWS);
        } else {
            itemCount = (int)m_numLoadedRows;
        }
        if (m_table.getItemCount() != itemCount) {
            m_table.setItemCount(itemCount);
        }

        DataRow row = null;
        if (index == m_currentIndex) {
            m_currentIndex++;
            row = m_it.next();
        } else {
            //reset row iterator and go to required index
            closeIterator();
            m_it = m_dataTable.iterator();
            for (m_currentIndex = 0; m_currentIndex <= index; m_currentIndex++) {
                row = m_it.next();
            }
        }
        if (row != null) {
            item.setText(0, row.getKey().getString());
            //get right column count: without id column (and 'remaining column skipped'-column)
            int colCount = m_table.getColumnCount() == MAX_NUM_COLUMN ? m_table.getColumnCount() - 2
                : m_table.getColumnCount() - 1;
            for (int i = 0; i < colCount; i++) {
                DataCell c = row.getCell(i);
                String s = c.toString().replaceAll("\\p{Cntrl}", "_");
                item.setText(i + 1, s);
            }
            if (m_table.getColumnCount() == MAX_NUM_COLUMN) {
                item.setText(MAX_NUM_COLUMN - 1, "...");
            }
        } else {
            item.setText("Row " + m_currentIndex + " could not be read.");
        }
    }

    long getNumRows() {
        return m_numTotalRows;
    }

    int getNumColumns() {
        return m_numTotalCols;
    }

    private class AddDataRowListener implements Listener {

        @Override
        public void handleEvent(final Event event) {
            TableItem item = (TableItem)event.item;
            fillTableItem(item);
        }
    }
}
