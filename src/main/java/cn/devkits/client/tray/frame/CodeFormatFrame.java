package cn.devkits.client.tray.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.devkits.client.tray.frame.assist.TextLineNumber;
import cn.devkits.client.util.DKStringUtil;

/**
 * 
 * 代码格式化
 * @author shaofeng liu
 * @version 1.0.0
 * @time 2019年11月25日 下午11:11:04
 */
public class CodeFormatFrame extends DKAbstractFrame {

    private static final long serialVersionUID = -3324482544348779089L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeFormatFrame.class);

    private JSplitPane currentComponent;

    public CodeFormatFrame() {
        super("Code Format");

        initUI(getRootPane());
        initListener();
    }


    @Override
    protected void initUI(JRootPane jRootPane) {
        jRootPane.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        addTabContent(tabbedPane, "Json Format");
        addTabContent(tabbedPane, "XML Format");
        tabbedPane.setPreferredSize(new Dimension(WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT));

        jRootPane.add(tabbedPane, BorderLayout.CENTER);
    }


    @Override
    protected void initListener() {
        /**
         * add resize listener, change divider location when windows resized.
         */
        super.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                currentComponent.setDividerLocation(getWidth() / 2);
            }
        });

        super.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (e.getComponent() instanceof CodeFormatFrame) {
                    CodeFormatFrame frame = (CodeFormatFrame) e.getComponent();
                    // TODO
                }
            }
        });
    }



    private void addTabContent(JTabbedPane tabbedPane, String title) {
        currentComponent = new JSplitPane();

        JTextArea leftTextArea = new JTextArea("Ugly String(Auto format after key release.)");
        JTextArea rightTextArea = new JTextArea("Format String");

        leftTextArea.addKeyListener(new JsonKeyListener(rightTextArea, title));

        JScrollPane leftScrollPane = new JScrollPane(leftTextArea);
        leftScrollPane.setRowHeaderView(new TextLineNumber(leftTextArea));
        currentComponent.setLeftComponent(leftScrollPane);

        JScrollPane rightScrollPane = new JScrollPane(rightTextArea);
        rightScrollPane.setRowHeaderView(new TextLineNumber(rightTextArea));
        currentComponent.setRightComponent(rightScrollPane);

        currentComponent.setDividerLocation(WINDOW_SIZE_WIDTH / 2);

        tabbedPane.addTab(title, currentComponent);
        tabbedPane.setEnabledAt(0, true);
    }


    class JsonKeyListener extends KeyAdapter {
        private JTextArea rightTextArea;
        private String title;

        public JsonKeyListener(JTextArea rightTextArea, String title) {
            this.rightTextArea = rightTextArea;
            this.title = title;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            JTextArea left = (JTextArea) e.getSource();
            String text = left.getText();

            if (text == null || text.trim().isEmpty()) {
                return;
            }

            String formatStr = "";
            if ("Json Format".equals(title)) {
                formatStr = DKStringUtil.jsonFormat(text);
            } else if ("XML Format".equals(title)) {
                formatStr = DKStringUtil.xmlFormat(text);
            }
            if (formatStr.startsWith("Invalid")) {
                JOptionPane.showMessageDialog(currentComponent, formatStr);
            } else {
                rightTextArea.setText(formatStr);
            }
        }
    }

    public JSplitPane getCurrentComponent() {
        return currentComponent;
    }
}
