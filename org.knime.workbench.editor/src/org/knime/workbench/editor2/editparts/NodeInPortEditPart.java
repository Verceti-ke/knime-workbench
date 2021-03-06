/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.workbench.editor2.figures.NodeInPortFigure;

/**
 * Edit Part for a {@link NodeInPort}. Model: {@link NodeInPort} View:
 * {@link NodeInPortFigure} Controller: {@link NodeInPortEditPart}
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodeInPortEditPart extends AbstractPortEditPart {
    /**
     * @param type the type of the port
     * @param portID The id for this incoming port
     */
    public NodeInPortEditPart(final PortType type, final int portID) {
        super(type, portID, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainerUI container = getNodeContainer();
        boolean isMetaNode = !(container instanceof SingleNodeContainerUI);
        NodeInPortFigure portFigure =
                new NodeInPortFigure(getType(), getIndex(), container
                        .getNrInPorts(), isMetaNode, container.getInPort(
                        getIndex()).getPortName());
        portFigure.setIsConnected(isConnected());
        return portFigure;
    }

    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     *         {@inheritDoc}
     */
    @Override
    public List<ConnectionContainerUI> getModelTargetConnections() {
        if (getManager() == null) {
            return EMPTY_LIST;
        }
        if (!getManager().containsNodeContainer(getID())) {
            return EMPTY_LIST;
        }
        ConnectionContainerUI container =
                getManager().getIncomingConnectionFor(
                        getNodeContainer().getID(), getIndex());

        if (container != null) {
            return Collections.singletonList(container);
        }

        return EMPTY_LIST;
    }

    /**
     * @return empty list, as in-ports are never source for connections
     *
     *         {@inheritDoc}
     */
    @Override
    protected List<ConnectionContainerUI> getModelSourceConnections() {
        return EMPTY_LIST;
    }

}
