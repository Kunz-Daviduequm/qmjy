package cn.devkits.client.tray.frame;

import cn.devkits.client.tray.frame.asyn.SearchFileThread;
import cn.devkits.client.tray.model.LargeDuplicateFilesTableModel;
import cn.devkits.client.util.DKDateTimeUtil;
import cn.devkits.client.util.DKFileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.SpringLayout.Constraints;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * 重复大文件检查<br>
 * Oracle Swing DEMO:https://docs.oracle.com/javase/tutorial/uiswing/examples/components/index.html#
 * GlassPaneDemo
 *
 * @author Shaofeng Liu
 * @version 1.0.0
 * @time 2019年9月26日 下午9:34:49
 */
public class LargeDuplicateFilesFrame extends DKAbstractFrame {

    public static final String[] BUTTONS_TEXT = {"Start Detect", "Stop Detect"};

    private static final long serialVersionUID = 6081895254576694963L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LargeDuplicateFilesFrame.class);

    private static final String[] FILE_TYPE_UNITS = {"All", "Document", "Image", "Audio", "Video"};
    private static final String[] FILE_UNITS = {"Byte", "KB", "MB", "GB", "TB", "PB"};

    private static final int COMPONENT_MARGIN_TOP_BASE = 5;
    private static final int COMPONENT_MARGIN_TOP_LABLE = 10;
    private static final int COMPONENT_MARGIN_RIGHT = 10;

    /**
     * 端口检查线程，充分利用CPU，尽量让IO吞吐率达到最大阈值
     */
    public static final int FIXED_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 10;

    private JTree tree = null;
    private JPopupMenu jtreeMenu = null;
    private JTable table = new JTable();
    private JLabel statusLine = null;
    private JTextField searchPath;
    private JComboBox<String> fileTypeComboBox = null;
    private JTextField minFileSizeInput = null;
    private JTextField maxFileSizeInput = null;
    private JComboBox<String> fileSizeUnitComboBox = null;
    private JButton startCancelBtn = null;
    private JButton exportBtn = null;
    private ExecutorService theadPool = null;

    private DefaultMutableTreeNode treeNode = null;
    private DefaultTreeModel treeModel = null;
    //https://www.cnblogs.com/blueskyli/p/9816324.html    fileSize:fileMd5:filePath
    private Map<Long, Map<String, Set<String>>> fileLengMd5Map = new ConcurrentHashMap<>();


    public LargeDuplicateFilesFrame() {
        super("Large Duplicate Files", 1.2f);

        initUI(getContentPane());
        initListener();
    }

    @Override
    protected void initUI(Container rootContainer) {
        rootContainer.add(initNorthPane(), BorderLayout.NORTH);

        JSplitPane jSplitPane = new JSplitPane();

        treeNode = new DefaultMutableTreeNode("Duplicate Files");
        treeModel = new DefaultTreeModel(treeNode);
        tree = new JTree(treeModel);
        initPopupMenu();

        // ToolTipManager.sharedInstance().registerComponent(tree);
        // TreeCellRenderer renderer = new LargeDuplicateFilesTreeCellRenderer();
        // tree.setCellRenderer(renderer);

        JScrollPane scrollPane = new JScrollPane(tree);
        jSplitPane.setLeftComponent(scrollPane);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);// 列自适应
        jSplitPane.setRightComponent(new JScrollPane(table));

        jSplitPane.setResizeWeight(0.3);
        rootContainer.add(jSplitPane, BorderLayout.CENTER);

        statusLine = new JLabel("Ready to go...");
        statusLine.setPreferredSize(new Dimension(WINDOW_SIZE_WIDTH, 25));
        rootContainer.add(statusLine, BorderLayout.SOUTH);

        initDataModel();
    }

    private void initPopupMenu() {
        this.jtreeMenu = new JPopupMenu();

        jtreeMenu.add(new JMenuItem("Open"));
        jtreeMenu.addSeparator();
        jtreeMenu.add(new JMenuItem("Delete"));
    }

    private JPanel initNorthPane() {
        JPanel northRootPane = new JPanel();
        SpringLayout mgr = new SpringLayout();
        northRootPane.setLayout(mgr);

        JLabel fileSearchPathLbl = new JLabel("Search Path:", JLabel.LEFT);
        this.searchPath = new JTextField();
        searchPath.setColumns(15);
        searchPath.setText("Computer");

        JLabel fileTypeLabel = new JLabel("File Type: ", JLabel.RIGHT);
        fileTypeComboBox = new JComboBox<String>(FILE_TYPE_UNITS);
        fileTypeComboBox.setLightWeightPopupEnabled(false);

        JLabel minSizeLabel = new JLabel("Minimum: ", JLabel.RIGHT);
        minFileSizeInput = new JTextField(2);
        minFileSizeInput.setText("0");

        JLabel maxSizeLabel = new JLabel("Maximum: ", JLabel.RIGHT);
        maxFileSizeInput = new JTextField(2);

        JLabel fileSizeUnit = new JLabel("Size Unit: ", JLabel.RIGHT);
        fileSizeUnitComboBox = new JComboBox<String>(FILE_UNITS);
        fileSizeUnitComboBox.setLightWeightPopupEnabled(false);
        fileSizeUnitComboBox.setSelectedIndex(2);// 默认选中MB单位

        startCancelBtn = new JButton(BUTTONS_TEXT[0]);
        startCancelBtn.setFocusPainted(false);// 不显示焦点虚线边框

        exportBtn = new JButton("Export");
        exportBtn.setFocusPainted(false);// 不显示焦点虚线边框

        northRootPane.add(fileSearchPathLbl);
        northRootPane.add(searchPath);
        northRootPane.add(fileTypeLabel);
        northRootPane.add(fileTypeComboBox);
        northRootPane.add(minSizeLabel);
        northRootPane.add(minFileSizeInput);
        northRootPane.add(maxSizeLabel);
        northRootPane.add(maxFileSizeInput);
        northRootPane.add(fileSizeUnit);
        northRootPane.add(fileSizeUnitComboBox);
        northRootPane.add(startCancelBtn);
        northRootPane.add(exportBtn);

        Constraints searchFilePathLbl = mgr.getConstraints(fileSearchPathLbl);
        searchFilePathLbl.setX(Spring.constant(COMPONENT_MARGIN_RIGHT));
        searchFilePathLbl.setY(Spring.constant(COMPONENT_MARGIN_TOP_LABLE));

        Constraints searchFilePathTextFieldCons = mgr.getConstraints(searchPath);
        searchFilePathTextFieldCons.setConstraint(SpringLayout.WEST, Spring.sum(searchFilePathLbl.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        searchFilePathTextFieldCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints fileTypeLabelCons = mgr.getConstraints(fileTypeLabel);
        fileTypeLabelCons.setConstraint(SpringLayout.WEST, Spring.sum(searchFilePathTextFieldCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        fileTypeLabelCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_LABLE));

        Constraints fileTypeComboCons = mgr.getConstraints(fileTypeComboBox);
        fileTypeComboCons.setConstraint(SpringLayout.WEST, Spring.sum(fileTypeLabelCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        fileTypeComboCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints minSizeLabelCons = mgr.getConstraints(minSizeLabel);
        minSizeLabelCons.setConstraint(SpringLayout.WEST, Spring.sum(fileTypeComboCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        minSizeLabelCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_LABLE));

        Constraints minFileSizeInputCons = mgr.getConstraints(minFileSizeInput);
        minFileSizeInputCons.setConstraint(SpringLayout.WEST, Spring.sum(minSizeLabelCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        minFileSizeInputCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints maxSizeLabelCons = mgr.getConstraints(maxSizeLabel);
        maxSizeLabelCons.setConstraint(SpringLayout.WEST, Spring.sum(minFileSizeInputCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        maxSizeLabelCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_LABLE));

        Constraints maxFileSizeInputCons = mgr.getConstraints(maxFileSizeInput);
        maxFileSizeInputCons.setConstraint(SpringLayout.WEST, Spring.sum(maxSizeLabelCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        maxFileSizeInputCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints fileSizeUnitCons = mgr.getConstraints(fileSizeUnit);
        fileSizeUnitCons.setConstraint(SpringLayout.WEST, Spring.sum(maxFileSizeInputCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        fileSizeUnitCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_LABLE));

        Constraints fileSizeUnitComboBoxCons = mgr.getConstraints(fileSizeUnitComboBox);
        fileSizeUnitComboBoxCons.setConstraint(SpringLayout.WEST, Spring.sum(fileSizeUnitCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        fileSizeUnitComboBoxCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints startCancelBtnCons = mgr.getConstraints(startCancelBtn);
        startCancelBtnCons.setConstraint(SpringLayout.WEST, Spring.sum(fileSizeUnitComboBoxCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        startCancelBtnCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        Constraints exportBtnCons = mgr.getConstraints(exportBtn);
        exportBtnCons.setConstraint(SpringLayout.WEST, Spring.sum(startCancelBtnCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        exportBtnCons.setY(Spring.constant(COMPONENT_MARGIN_TOP_BASE));

        SpringLayout.Constraints panelCons = mgr.getConstraints(northRootPane);
        panelCons.setConstraint(SpringLayout.SOUTH, Spring.sum(exportBtnCons.getConstraint(SpringLayout.SOUTH), Spring.constant(COMPONENT_MARGIN_TOP_BASE)));
        panelCons.setConstraint(SpringLayout.WEST, Spring.sum(exportBtnCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));
        panelCons.setConstraint(SpringLayout.EAST, Spring.sum(exportBtnCons.getConstraint(SpringLayout.EAST), Spring.constant(COMPONENT_MARGIN_RIGHT)));

        return northRootPane;
    }


    @Override
    protected void initListener() {
        // 窗口关闭时释放线程池资源
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (theadPool != null && !theadPool.isShutdown()) {
                    theadPool.shutdownNow();
                }
            }
        });

        startCancelBtn.addActionListener(new StartEndListener(this));
        exportBtn.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jfc.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
            jfc.setDialogTitle("Select Export Path");
            int i = jfc.showSaveDialog(this);
            if (i == JFileChooser.APPROVE_OPTION) {
                File file = jfc.getSelectedFile();
                exportResult(file);
            }
        });

        // 左侧树单选事件
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                JTree tree = (JTree) e.getSource();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                Set<String> filesSet = null;
                if (node != null) {
                    if (node.getLevel() == 0) {
                        return;
                    } else if (node.getLevel() == 1) {
                        String md5 = node.getUserObject().toString();
                        filesSet = getFileSet(md5);
                    } else if (node.getLevel() == 2) {
                        filesSet = new HashSet<String>();
                        filesSet.add(node.getUserObject().toString());
                    }
                }

                if (filesSet != null) {
                    updateTableData(filesSet);
                }
            }
        });

        // 叶子节点双击打开文件所在目录
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {// 鼠标左键
                    JTree tree = (JTree) e.getSource();
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (e.getClickCount() == 2 && node.getLevel() == 2) {
                        File file = new File(node.getUserObject().toString());
                        DKFileUtil.openFile(file.getParentFile());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                TreePath tp = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (tp != null) {
                    tree.setSelectionPath(tp);
                }
                jtreeMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private Set<String> getFileSet(String md5) {
        Iterator<Map<String, Set<String>>> iterator = fileLengMd5Map.values().iterator();
        while (iterator.hasNext()) {
            Map<String, Set<String>> next = iterator.next();
            Iterator<String> md5Iter = next.keySet().iterator();
            while (md5Iter.hasNext()) {
                if (md5.equals(md5Iter.next())) {
                    return next.get(md5);
                }
            }
        }
        return Sets.newHashSet();
    }

    private void exportResult(File saveFolder) {
        File exportFile = new File(saveFolder.getAbsolutePath() + File.separator + "DevkitsDuplicateFiles_" + DKDateTimeUtil.currentTimeStr() + ".csv");
        flushResult(exportFile, Lists.newArrayList(new String[]{"File Name, MD5, File Extension, File Path, File Size, Create Time"}));

        DefaultTreeModel rootModel = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) rootModel.getRoot();

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            List<String> data = new ArrayList<>();

            DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) children.nextElement();
            String md5 = childTreeNode.getUserObject().toString();
            Enumeration children1 = childTreeNode.children();
            while (children1.hasMoreElements()) {
                File f = new File(children1.nextElement().toString());
                StringBuilder sb = new StringBuilder();
                sb.append(f.getName()).append(",").append(md5).append(",").append(FilenameUtils.getExtension(f.getName())).append(",")
                        .append(f.getAbsolutePath()).append(",").append(DKFileUtil.formatBytes(f.length())).append(",")
                        .append(DKDateTimeUtil.getDatetimeStrOfLong(f.lastModified(), "yyyy-MM-dd HH:mm:ss"));
                data.add(sb.toString());
            }
            flushResult(exportFile, data);
        }
    }

    private void flushResult(File exportFile, List<String> data) {
        try {
            FileUtils.writeLines(exportFile, data, true);
        } catch (IOException e) {
            LOGGER.error("Flush result to file '{}' failed: {}", exportFile.getAbsolutePath(), e.getMessage());
        }
    }

    public void initDataModel() {
        fileLengMd5Map.clear();

        treeNode.removeAllChildren();
        treeModel.reload();

        table.setModel(new LargeDuplicateFilesTableModel());

        theadPool = Executors.newFixedThreadPool(FIXED_THREAD_NUM);
    }

    public void searchComplete() {
        theadPool.shutdown();
        updateStatusLineText("Files Search Completed!");
        startCancelBtn.setText(BUTTONS_TEXT[0]);
    }

    public void updateStatusLineText(final String text) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                statusLine.setText(text);
            }
        });
    }

    public void updateTableData(Set<String> files) {
        table.setModel(new LargeDuplicateFilesTableModel(files));
    }

    public void updateTreeData(long length, String md5, String file) {
        Map<String, Set<String>> sameLengthFiles = fileLengMd5Map.get(length);
        if (sameLengthFiles == null) {
            sameLengthFiles = new ConcurrentHashMap<>();
            fileLengMd5Map.put(length, sameLengthFiles);
        }

        Set<String> md5FilesSet = sameLengthFiles.get(md5);
        if (md5FilesSet == null) {
            md5FilesSet = new HashSet<>();
            md5FilesSet.add(file);
            sameLengthFiles.put(md5, md5FilesSet);
        } else {
            if (md5FilesSet.size() == 1) {
                insertTreeNode(md5, md5FilesSet.iterator().next());
            }
            md5FilesSet.add(file);
            insertTreeNode(md5, file);
        }
    }

    private void insertTreeNode(String md5, String file) {
        DefaultTreeModel rootModel = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) rootModel.getRoot();

        int childCount = treeNode.getChildCount();
        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) children.nextElement();
            if (childTreeNode.getUserObject().toString().equals(md5)) {
                rootModel.insertNodeInto(new DefaultMutableTreeNode(file), childTreeNode, childTreeNode.getChildCount());
                return;
            }
        }

        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(md5);
        rootModel.insertNodeInto(newChild, treeNode, childCount);
        rootModel.insertNodeInto(new DefaultMutableTreeNode(file), newChild, newChild.getChildCount());
    }


    public JComboBox<String> getFileTypeComboBox() {
        return fileTypeComboBox;
    }

    public JTextField getMinFileSizeInput() {
        return minFileSizeInput;
    }

    public JTextField getMaxFileSizeInput() {
        return maxFileSizeInput;
    }

    public JComboBox<String> getFileSizeUnitComboBox() {
        return fileSizeUnitComboBox;
    }

    public ExecutorService getTheadPool() {
        return theadPool;
    }

    public JButton getStartCancelBtn() {
        return startCancelBtn;
    }

    public JTextField getSearchPath() {
        return searchPath;
    }

    public Map<Long, Map<String, Set<String>>> getFileLengMd5Map() {
        return fileLengMd5Map;
    }
}


/**
 * 启动、取消按钮事件监听
 *
 * @author shaofeng liu
 * @version 1.0.0
 * @datetime 2019年10月5日 下午3:25:08
 */
class StartEndListener implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartEndListener.class);

    private LargeDuplicateFilesFrame frame;

    public StartEndListener(LargeDuplicateFilesFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton btn = (JButton) e.getSource();
        ExecutorService threadPool = frame.getTheadPool();

        if (LargeDuplicateFilesFrame.BUTTONS_TEXT[0].equals(e.getActionCommand())) {
            if (threadPool.isShutdown()) {
                frame.initDataModel();
            }
            new Thread(new SearchFileThread(frame)).start();
            btn.setText(LargeDuplicateFilesFrame.BUTTONS_TEXT[1]);
            frame.updateStatusLineText("Start to scanner File...");
        } else {
            threadPool.shutdownNow();
            btn.setText(LargeDuplicateFilesFrame.BUTTONS_TEXT[0]);
            frame.updateStatusLineText("Scanner file canceled by user!");
        }
    }
}
