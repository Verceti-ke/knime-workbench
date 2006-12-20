/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;

import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import org.knime.workbench.editor2.editparts.policy.NewConnectionComponentEditPolicy;
import org.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
        implements WorkflowListener {
    private final boolean m_isModelPortConnection;

    /**
     * The constructor.
     * 
     * @param isModelConn a flag telling if this is a connection between model
     *            ports or not.
     */
    public ConnectionContainerEditPart(final boolean isModelConn) {
        m_isModelPortConnection = isModelConn;
    }

    /**
     * Creates a GEF command to shift the connections bendpoints.
     * 
     * @param request the underlying request holding information about the shift
     * @return the command to change the bendpoint locations
     */
    public Command getBendpointAdaptionCommand(final Request request) {

        ChangeBoundsRequest boundsRequest = (ChangeBoundsRequest)request;

        ZoomManager zoomManager = (ZoomManager)(getRoot().getViewer()
                .getProperty(ZoomManager.class.toString()));

        Point moveDelta = boundsRequest.getMoveDelta();
        return new ChangeBendPointLocationCommand(
                (ConnectionContainer)getModel(), moveDelta, zoomManager);
    }

    /**
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();
        ((ConnectionContainer)getModel()).addWorkflowListener(this);
    }

    /**
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    @Override
    public void deactivate() {
        super.deactivate();
        ((ConnectionContainer)getModel()).removeWorkflowListener(this);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    @Override
    protected void createEditPolicies() {

        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
                new ConnectionEndpointEditPolicy());

        // enable bendpoints (must be stored in extra info)
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE,
                new ConnectionBendpointEditPolicy());

        installEditPolicy(EditPolicy.CONNECTION_ROLE,
                new NewConnectionComponentEditPolicy());

    }

    /**
     * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#createFigure()
     */
    @Override
    protected IFigure createFigure() {

        PolylineConnection conn = (PolylineConnection)super.createFigure();

        // Bendpoints
        SnapOffBendPointConnectionRouter router = new SnapOffBendPointConnectionRouter();
        conn.setConnectionRouter(router);
        conn.setRoutingConstraint(new ArrayList());

        // Decorations
        //PolygonDecoration pD = new PolygonDecoration();
        if (m_isModelPortConnection) {
            // pD.setScale(9, 5);
            conn.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
            conn.setLineWidth(1);
        }

        //conn.setTargetDecoration(pD);

        return conn;
    }

    /**
     * @see org.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(org.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        refreshVisuals();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        ModellingConnectionExtraInfo ei = null;
        ei = (ModellingConnectionExtraInfo)((ConnectionContainer)getModel())
                .getExtraInfo();
        if (ei == null) {
            return;
        }

        Connection fig = (Connection)getFigure();
        // recreate list of bendpoints
        int[][] p = ei.getAllBendpoints();
        ArrayList<AbsoluteBendpoint> constraint = new ArrayList<AbsoluteBendpoint>();
        for (int i = 0; i < p.length; i++) {
            AbsoluteBendpoint bp = new AbsoluteBendpoint(p[i][0], p[i][1]);
            constraint.add(bp);
        }

        fig.setRoutingConstraint(constraint);
    }
}
