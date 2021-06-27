package cn.devkits.client.tray.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.Optional;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import cn.devkits.client.tray.model.FileTableModel;
import cn.devkits.client.util.DKFileUtil;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class FileExplorerPanel extends JPanel {

    /** serialVersionUID */
    private static final long serialVersionUID = -2230863950855742735L;
    private JButton backBtn;
    private JTextField currentPathTextField;
    private JButton forwardBtn;
    private JTable filesTable;
    private JLabel statusBar;
    private LinkedList<String> history = new LinkedList<String>();
    private int historyIndex = -1;

    private String newPath = System.getProperty("user.home");
    private FileTableModel model;

    public FileExplorerPanel() {
        super(new BorderLayout());

        initUI();
        initListener();
    }

    private void initUI() {
        Icon leftIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_LEFT, 16, new Color(50, 50, 50));
        this.backBtn = new JButton(leftIcon);
        backBtn.setToolTipText("Back");
        backBtn.setEnabled(false);

        this.currentPathTextField = new JTextField(newPath);

        Icon rightIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_RIGHT, 16, new Color(50, 50, 50));
        this.forwardBtn = new JButton(rightIcon);
        forwardBtn.setToolTipText("Forward");

        JPanel jPanel = new JPanel();
        SpringLayout layout = new SpringLayout();
        jPanel.setLayout(layout);

        jPanel.add(backBtn);
        jPanel.add(currentPathTextField);
        jPanel.add(forwardBtn);

        SpringLayout.Constraints buttonCons = layout.getConstraints(backBtn);
        buttonCons.setX(Spring.constant(5));
        buttonCons.setY(Spring.constant(5));

        SpringLayout.Constraints textFieldCons = layout.getConstraints(currentPathTextField);
        textFieldCons.setX(Spring.sum(Spring.constant(5), buttonCons.getConstraint(SpringLayout.EAST)));
        textFieldCons.setY(Spring.constant(5));

        SpringLayout.Constraints forwardBtnCons = layout.getConstraints(forwardBtn);
        forwardBtnCons.setX(Spring.sum(Spring.constant(5), textFieldCons.getConstraint(SpringLayout.EAST)));
        forwardBtnCons.setY(Spring.constant(5));

        // Adjust constraints for the content pane.
        setContainerSize(jPanel, 5);

        add(jPanel, BorderLayout.NORTH);

        this.model = new FileTableModel(new File(newPath));
        this.filesTable = new JTable(model);
        add(new JScrollPane(filesTable), BorderLayout.CENTER);

        this.statusBar = new JLabel("Read to go...");
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setContainerSize(Container parent, int pad) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component[] components = parent.getComponents();
        Spring maxHeightSpring = Spring.constant(0);
        SpringLayout.Constraints pCons = layout.getConstraints(parent);

        // Set the container's right edge to the right edge
        // of its rightmost component + padding.
        Component rightmost = components[components.length - 1];
        SpringLayout.Constraints rCons = layout.getConstraints(rightmost);
        pCons.setConstraint(SpringLayout.EAST, Spring.sum(Spring.constant(pad), rCons.getConstraint(SpringLayout.EAST)));

        // Set the container's bottom edge to the bottom edge
        // of its tallest component + padding.
        for (int i = 0; i < components.length; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(components[i]);
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getConstraint(SpringLayout.SOUTH));
        }
        pCons.setConstraint(SpringLayout.SOUTH, Spring.sum(Spring.constant(pad), maxHeightSpring));
    }


    private void initListener() {
        currentPathTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 判断按下的键是否是回车键
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loadUserInput();
                }
            }
        });

        filesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int rowNum = filesTable.getSelectedRow();
                    String fileName = (String) filesTable.getValueAt(rowNum, 0);
                    updateStatusBar(fileName);
                } else if (e.getClickCount() == 2) {
                    int rowNum = filesTable.getSelectedRow();
                    String fileName = (String) filesTable.getValueAt(rowNum, 0);
                    openFileOrDir(fileName);
                }
            }
        });

        backBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (historyIndex > 0) {
                    backBtn.setEnabled(true);
                } else {
                    backBtn.setEnabled(false);
                }

                String path = history.get(historyIndex);
                historyIndex--;
                loadDirFilesWithoutHistory(path);
            }
        });

        forwardBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loadUserInput();
            }
        });
    }

    private void loadUserInput() {
        String text = currentPathTextField.getText();
        if (text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(currentPathTextField, "The input file path is empty!");
        }

        File file = new File(text.trim());
        if (!file.exists()) {
            JOptionPane.showMessageDialog(currentPathTextField, "The input file path is invalid!");
        }

        if (file.isDirectory()) {
            loadDirFiles(file, false);
        } else {
            DKFileUtil.openFile(file);
        }
    }

    private void loadDirFiles(File newFileDir, boolean needUpdateTextField) {
        String path = newFileDir.getAbsolutePath();

        history.add(path);
        historyIndex++;
        backBtn.setEnabled(!history.isEmpty());

        updateUI(path, needUpdateTextField);
    }

    private void loadDirFilesWithoutHistory(String filePath) {
        updateUI(filePath, true);
    }

    private void updateUI(String path, boolean needUpdateTextField) {
        if (needUpdateTextField) {
            currentPathTextField.setText(path);
        }

        model.updateRoot(new File(path));
        filesTable.validate();
        filesTable.updateUI();
    }

    private void updateStatusBar(String fileName) {
        Optional<File> selectFile = getSelectFile(fileName);
        if (selectFile.isPresent()) {
            statusBar.setText(selectFile.get().getAbsolutePath());
        }
    }

    private Optional<File> getSelectFile(String fileName) {
        File[] fileLists = new File(currentPathTextField.getText()).listFiles();
        for (File file : fileLists) {
            if (file.getAbsolutePath().endsWith(fileName)) {
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }


    private void openFileOrDir(String fileName) {
        Optional<File> selectFile = getSelectFile(fileName);
        if (selectFile.isPresent()) {
            File file = selectFile.get();
            if (file.isDirectory()) {
                loadDirFiles(file, true);
            } else {
                DKFileUtil.openFile(file);
            }
        }
    }

}
