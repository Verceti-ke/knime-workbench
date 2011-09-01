/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
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
 */
package org.knime.workbench.explorer.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Content and Label provider for the explorer view. Delegates the corresponding
 * calls to different content providers providing a view to different sources.
 * <br />
 * The objects returned by the different providers are wrapped into a
 * {@link ContentObject} (associating the creating provider with it) and the
 * wrapper is placed in the tree view. <br />
 * An instance of this should be set as input of the tree view.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class ContentDelegator extends LabelProvider implements
        IStructuredContentProvider, ITreeContentProvider,
        IPropertyChangeListener {
    /**
     * The property for changes in the content IPropertyChangeListener can
     * register for.
     */
    public static final String CONTENT_CHANGED = "CONTENT_CHANGED";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ContentDelegator.class);

    private final List<IPropertyChangeListener> m_changeListener;

    private static final Image USER_SPACE_IMG = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/workflow_projects.png").createImage();

    /**
     * constant empty array. Yup.
     */
    private static final ContentObject[] NO_CHILDREN = new ContentObject[0];

    /**
     * All currently visible providers.
     */
    private final HashSet<MountPoint> m_provider;

    /**
     * Creates a new content delegator and registers it for property changes of
     * the explorer mount table. None of the mounted content is visible through
     * this constructor.
     */
    public ContentDelegator() {
        this(false);
    }

    /**
     * Creates a new content delegator and registers it for property changes of
     * the explorer mount table. All of the currently mounted content is visible
     * if the parameter is set true.
     *
     * @param showAll if true all currently mounted content is visible through
     *            this content provider. If false, the content is empty.
     */
    public ContentDelegator(final boolean showAll) {
        m_provider = new LinkedHashSet<MountPoint>();
        if (showAll) {
            for (String id : ExplorerMountTable.getAllMountIDs()) {
                MountPoint mp = ExplorerMountTable.getMountPoint(id);
                if (mp != null) {
                    m_provider.add(mp);
                }
            }
        }
        m_changeListener = new CopyOnWriteArrayList<IPropertyChangeListener>();
        ExplorerMountTable.addPropertyChangeListener(this);
    }

    /**
     * Adds the specified content provider to the explorer.
     *
     * @param mountPoint the mount point to add
     */
    public void addMountPoint(final MountPoint mountPoint) {
        if (mountPoint == null) {
            throw new NullPointerException("Mount point can't be null");
        }
        m_provider.add(mountPoint);
        notifyListeners(new PropertyChangeEvent(mountPoint, CONTENT_CHANGED,
                null, mountPoint.getMountID()));
    }

    /**
     * @return a set with the ids of the currently shown mount points
     */
    public Set<String> getMountedIds() {
        Set<String> mounted = new LinkedHashSet<String>();
        for (MountPoint mountPoint : m_provider) {
            mounted.add(mountPoint.getMountID());
        }
        return mounted;
    }

    /**
     * Clears the view content.
     */
    public void removeAllMountPoints() {
        m_provider.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_provider.clear();
        ExplorerMountTable.removePropertyChangeListener(this);
        super.dispose();
    }

    /**
     * @return a list of providers currently visible in this view
     */
    public Collection<AbstractContentProvider> getVisibleContentProvider() {
        ArrayList<AbstractContentProvider> result =
                new ArrayList<AbstractContentProvider>();
        for (MountPoint mp : m_provider) {
            result.add(mp.getProvider());
        }
        return result;
    }

    /**
     * Converts an array of objects into an array of wrapped objects.
     *
     * @param provider the creator of the passed objects.
     * @param fileStores the explorer file stores to wrap
     * @return a new array with wrapped objects.
     */
    private ContentObject[] wrapObjects(final AbstractContentProvider provider,
            final AbstractExplorerFileStore[] fileStores) {
        ContentObject[] result = new ContentObject[fileStores.length];
        for (int i = 0; i < fileStores.length; i++) {
            result[i] = new ContentObject(provider, fileStores[i]);
        }
        return result;
    }

    /*
     * ------------ Content Provider Methods --------------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        // we are the root element - providers are the first level children
        if (parentElement == this) {
            return getVisibleContentProvider().toArray();
        }
        if (parentElement instanceof AbstractContentProvider) {
            AbstractContentProvider prov =
                    (AbstractContentProvider)parentElement;
            // get the children of the provider's root.
            return wrapObjects(prov, prov.getChildren(prov.getFileStore("/")));

        }
        if (!(parentElement instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + parentElement
                    + " of type "
                    + parentElement.getClass().getCanonicalName());
            return NO_CHILDREN;
        }
        ContentObject c = ((ContentObject)parentElement);
        AbstractContentProvider prov = c.getProvider();
        return wrapObjects(prov, prov.getChildren(c.getObject()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "KNIME Explorer #" + System.identityHashCode(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        // thanks for letting me know.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParent(final Object element) {
        if (element == this) {
            // we are the root of the tree
            return null;
        }
        if (element instanceof AbstractContentProvider) {
            // content providers are the first level children
            return this;
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return null;
        }

        ContentObject c = (ContentObject)element;
        // we must ask the corresponding content provider
        AbstractContentProvider provider = c.getProvider();
        AbstractExplorerFileStore parent = provider.getParent(c.getObject());
        if (parent == null || provider.getFileStore("/").equals(parent)) {
            // the root of each subtree is the provider itself
            return provider;
        } else {
            return new ContentObject(provider, parent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element == this) {
            // content providers are the first level children
            return !m_provider.isEmpty();
        }
        if (element instanceof AbstractContentProvider) {
            // content providers are the first level children
            AbstractContentProvider prov = (AbstractContentProvider)element;
            return prov.hasChildren(prov.getFileStore("/"));
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return false;
        }
        ContentObject c = (ContentObject)element;
        return c.getProvider().hasChildren(c.getObject());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        // we are the root element - providers are the first level children
        if (inputElement == this) {
            return getVisibleContentProvider().toArray();
        }
        if (inputElement instanceof AbstractContentProvider) {
            AbstractContentProvider prov =
                    (AbstractContentProvider)inputElement;
            // get the children of the provider's root.
            return prov.getElements(prov.getFileStore("/"));
        }
        if (!(inputElement instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + inputElement
                    + " of type " + inputElement.getClass().getCanonicalName());
            return NO_CHILDREN;
        }
        ContentObject c = (ContentObject)inputElement;
        return c.getProvider().getElements(c.getObject());
    }

    /**
     * Creates the (new but) same object that is stored in the view tree for the
     * passed file.
     *
     * @param file to return the tree object for
     * @return the (new but) same object that is stored in the view tree for the
     *         passed file.
     */
    public static Object getTreeObjectFor(final AbstractExplorerFileStore file) {
        if (file == null) {
            return null;
        }
        String id = file.getMountID();
        MountPoint mp = ExplorerMountTable.getMountPoint(id);
        if (mp == null) {
            return null;
        }
        if (file.getFullName().equals("/") || file.getParent() == null) {
            return mp.getProvider();
        } else {
            return new ContentObject(mp.getProvider(), file);
        }
    }

    /**
     * Creates the (new but) same object that is stored in the view tree for the
     * mount point with the passed mount id.
     *
     * @param mountID mount id of the mount point
     * @return the (new but) same object that is stored in the view tree for the
     *         passed mount id.
     */
    public static AbstractContentProvider getTreeObjectFor(
            final String mountID) {
        MountPoint mp = ExplorerMountTable.getMountPoint(mountID);
        return mp.getProvider();
    }

    /**
     * Same as {@link #getTreeObjectFor(AbstractExplorerFileStore)}, just for
     * lists. The result list may be shorter than the argument list as null
     * elements are not added (in case objects in the argument collection are of
     * unexpected type).
     *
     * @param files the files to get tree objects for
     * @return a list of tree objects for the files.
     */
    public static List<Object> getTreeObjectList(
            final Collection<? extends AbstractExplorerFileStore> files) {
        if (files == null) {
            return null;
        }
        ArrayList<Object> result = new ArrayList<Object>();
        for (AbstractExplorerFileStore f : files) {
            Object o = getTreeObjectFor(f);
            if (o != null) {
                result.add(o);
            }
        }
        return result;
    }

    /**
     * Returns the file store corresponding to the passed treeviewer object.
     *
     * @param treeObject either a content object or a content provider
     * @return the file store corresponding to the passed treeviewer object or
     *         null, if the object passed is of unexpected type
     */
    public static AbstractExplorerFileStore getFileStore(
            final Object treeObject) {
        if (treeObject instanceof ContentObject) {
            return ((ContentObject)treeObject).getObject();
        } else if (treeObject instanceof AbstractContentProvider) {
            // content provider represent the root object of their content
            return ((AbstractContentProvider)treeObject).getFileStore("/");
        } else {
            return null;
        }
    }

    /**
     * Same as {{@link #getFileStore(Object)}. Just for collections. The result
     * list may be shorter than the argument list as null elements are not added
     * (in case objects in the argument collection are of unexpected type).
     *
     * @param treeObjects the tree objects to convert
     * @return a list of file stores
     */
    public static List<AbstractExplorerFileStore> getFileStoreList(
            final Collection<? extends Object> treeObjects) {
        if (treeObjects == null) {
            return null;
        }
        ArrayList<AbstractExplorerFileStore> result =
                new ArrayList<AbstractExplorerFileStore>();
        for (Object o : treeObjects) {
            AbstractExplorerFileStore f = getFileStore(o);
            if (f != null) {
                result.add(f);
            }
        }
        return result;
    }

    /*
     * ------------ Label Provider Methods --------------------
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object obj) {
        if (obj == this) {
            return USER_SPACE_IMG;
        }
        if (obj instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)obj).getImage();
        }
        if (!(obj instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + obj
                    + " of type " + obj.getClass().getCanonicalName());
            return null;
        }
        ContentObject c = (ContentObject)obj;
        return c.getProvider().getImage(c.getObject());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element == this) {
            return toString();
        }
        if (element instanceof AbstractContentProvider) {
            AbstractContentProvider acp = (AbstractContentProvider)element;
            return getMountID(acp) + " (" + acp.toString() + ")";
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return null;
        }
        ContentObject c = (ContentObject)element;
        return c.getProvider().getText(c.getObject());
    }

    private String getMountID(final AbstractContentProvider p) {
        for (MountPoint mp : m_provider) {
            if (mp.getProvider() == p) {
                return mp.getMountID();
            }
        }
        return "<?>";
    }

    /*
     * --------------------------------------------------------
     */

    // colon is very unlikely (up to not allowed) to appear in a mount id
    private static final char ID_SEP = ':';

    private static final String KEY = "DisplayedMountIDs";

    /**
     * @param storage store information in order to restore state
     */
    public void saveState(final IMemento storage) {
        StringBuilder sb = new StringBuilder();
        for (MountPoint mp : m_provider) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(mp.getMountID());
        }
        storage.putString(KEY, sb.toString());
    }

    /**
     *
     * Restore previously displayed mount points.
     *
     * @param storage to restore display from
     *
     */
    public void restoreState(final IMemento storage) {
        String displayed = storage.getString(KEY);
        if (displayed != null) {
            String[] ids = displayed.split(String.valueOf(ID_SEP));
            for (String id : ids) {
                MountPoint mp = ExplorerMountTable.getMountPoint(id);
                if (mp != null) {
                    addMountPoint(mp);
                } else {
                    LOGGER.info("Can't restore mount point to display: " + id);
                    continue;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (!ExplorerMountTable.MOUNT_POINT_PROPERTY
                .equals(event.getProperty())) {
            return;
        }
        if (event.getNewValue() == null) {
            // mount point was removed
            MountPoint mp = (MountPoint)event.getSource();
            boolean removed = m_provider.remove(mp);
            if (removed) {
                notifyListeners(new PropertyChangeEvent(mp, CONTENT_CHANGED,
                        mp.getMountID(), null));
                LOGGER.warn("Removed mount point with id \"" + mp.getMountID()
                        + "\" from view because it was deleted in the "
                        + "preferences.");
            }
        }
        // The addition of mount points is ignored.
    }

    /*---------------------------------------------------------------*/
    /**
     * Adds a property change listener for mount changes.
     *
     * @param listener the property change listener to add
     */
    public void addPropertyChangeListener(
            final IPropertyChangeListener listener) {
        m_changeListener.add(listener);
    }

    /**
     * Removes the given listener. Calling this method has no affect if the
     * listener is not registered.
     *
     * @param listener a property change listener
     */
    public void removePropertyChangeListener(
            final IPropertyChangeListener listener) {
        m_changeListener.remove(listener);
    }

    private void notifyListeners(final PropertyChangeEvent event) {
        for (IPropertyChangeListener listener : m_changeListener) {
            listener.propertyChange(event);
        }
    }
}
