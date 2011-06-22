package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Placeholder store in the tree for string messages.
 *
 * @author ohl, University of Konstanz
 */
public final class MessageFileStore extends ExplorerFileStore {

    private final String m_msg;

    /**
     * @param mountID
     * @param fullPath
     */
    public MessageFileStore(final String mountID, final String message) {
        super(mountID, "");
        if (message == null) {
            throw new NullPointerException(
                    "Message to display can't be null");
        }
        m_msg = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullName() {
        return m_msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toURI() {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore getMessageFileStore(final String msg) {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileInfo[] childInfos(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return EMPTY_FILE_INFO_ARRAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] childNames(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return EMPTY_STRING_ARRAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore[] childStores(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return new ExplorerFileStore[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyDirectory(final IFileInfo sourceInfo,
            final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyFile(final IFileInfo sourceInfo,
            final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int options, final IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileInfo fetchInfo() {
        // re-use Eclipse local file info
        FileInfo info = new FileInfo();
        info.setExists(false);
        info.setDirectory(false);
        info.setLastModified(0);
        info.setLength(0);
        info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
        info.setAttribute(EFS.ATTRIBUTE_HIDDEN, false);
        info.setName(m_msg);
        return info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileInfo fetchInfo(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return fetchInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileStore getChild(final IPath path) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileStore getFileStore(final IPath path) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore getChild(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentOf(final IFileStore other) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore mkdir(final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream(final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream(final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putInfo(final IFileInfo info, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(
                "Not supported in message file store.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_msg.hashCode();
    }
}