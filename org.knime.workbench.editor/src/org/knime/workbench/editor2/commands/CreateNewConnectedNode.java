/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: Mar 30, 2011
 * Author: ohl
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Creates a new node - and may auto connect it to another one.
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedNode extends Command {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNewConnectedNode.class);

    private final WorkflowManager m_manager;

    private final NodeFactory<?> m_factory;

    private final Point m_location;

    private final NodeContainer m_connectTo;

    private NodeID m_newNode;

    /**
     * Creates a new node and connects it to the passed node - if it fits.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location of the new node ABSOLTE COORDS!
     * @param connectTo node to which the new node should be connected to
     */
    public CreateNewConnectedNode(final WorkflowManager manager,
            final NodeFactory<? extends NodeModel> factory,
            final Point location, final NodeContainer connectTo) {
        m_manager = manager;
        m_factory = factory;
        m_location = location;
        m_connectTo = connectTo;
    }

    /**
     * We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_manager != null && m_factory != null && m_location != null;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {

        m_newNode = createNewNode();
        if (m_newNode != null && m_connectTo != null) {
            autoConnectNewNode();
        }

    }

    protected NodeID createNewNode() {
        // Add node to workflow and get the container
        NodeID newID = null;
        try {
            newID = m_manager.createAndAddNode(m_factory);
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return null;
        }
        // create extra info and set it
        NodeUIInformation info =
                new NodeUIInformation(m_location.x, m_location.y, -1, -1, true);
        NodeContainer nc = m_manager.getNodeContainer(newID);
        nc.setUIInformation(info);
        return newID;
    }

    protected void autoConnectNewNode() {
        if (m_newNode == null) {
            return;
        }
        if (m_connectTo == null) {
            return;
        }
        NodeContainer nc = m_manager.getNodeContainer(m_newNode);
        int[] matchingPorts = getMatchingPorts(m_connectTo, nc);
        if (matchingPorts == null) {
            LOGGER.info("Can't auto-connect new node (" + m_newNode + "): "
                    + "no matching port type found at node "
                    + m_connectTo.getNameWithID());
            return;
        }

        LOGGER.info("Autoconnect: Connecting new node " + m_newNode + " port "
                + matchingPorts[1] + " with existing node " + m_connectTo
                + " port " + matchingPorts[0]);
        try {
            m_manager.addConnection(m_connectTo.getID(), matchingPorts[0],
                    m_newNode, matchingPorts[1]).getID();
        } catch (Exception e) {
            String from = m_connectTo.getNameWithID();
            String to = nc.getNameWithID();
            String msg =
                    "Unable to add connection from " + from + " port "
                            + matchingPorts[0] + " to " + to + "port "
                            + matchingPorts[1] + ": " + e.getMessage();
            LOGGER.error(msg);
        }


    }

    private int[] getMatchingPorts(final NodeContainer left,
            final NodeContainer right) {
        // don't auto connect to flow var ports - start with port index 1
        int leftFirst = (left instanceof WorkflowManager) ? 0 : 1;
        int rightFirst = (right instanceof WorkflowManager) ? 0 : 1;
        int maybeLeft = -1;
        int maybeRight = -1;
        for (int rightPidx = rightFirst; rightPidx < right.getNrInPorts(); rightPidx++) {
            for (int leftPidx = leftFirst; leftPidx < left.getNrOutPorts(); leftPidx++) {
                NodeOutPort leftPort = left.getOutPort(leftPidx);
                NodeInPort rightPort = right.getInPort(rightPidx);
                if (leftPort.getPortType().isSuperTypeOf(
                        rightPort.getPortType())) {
                    if (m_manager.getOutgoingConnectionsFor(left.getID(),
                            leftPidx).size() == 0) {
                        // output not connected: use it.
                        return new int[]{leftPidx, rightPidx};
                    }
                    // port already connected - we MAY use it
                    if (maybeLeft < 0) {
                        maybeLeft = leftPidx;
                        maybeRight = rightPidx;
                    }
                }
            }
        }
        if (maybeLeft != -1) {
            return new int[]{maybeLeft, maybeRight};
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_newNode != null && m_manager.canRemoveNode(m_newNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        try {
            // blows away the connection as well.
            m_manager.removeNode(m_newNode);
        } catch (Exception e) {
            String msg =
                    "Undo failed: Node " + m_newNode + " can't be removed: "
                            + e.getMessage();
            LOGGER.error(msg);
            MessageDialog.openInformation(
                    Display.getDefault().getActiveShell(),
                    "Operation not allowed", msg);
        }
    }

}
