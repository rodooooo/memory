import java.applet.*;
import java.awt.*;
import java.util.*;

public class ControlPanel extends Frame {
    Kernel kernel;

    // Control buttons
    Button runButton = new Button("run");
    Button stepButton = new Button("step");
    Button resetButton = new Button("reset");
    Button exitButton = new Button("exit");

    private static final int PAGE_COUNT = 64;
    Button[] pageButtons = new Button[PAGE_COUNT];
    Label[] physicalLabels = new Label[PAGE_COUNT];

    // State labels
    Label statusValueLabel = new Label("STOP", Label.LEFT);
    Label timeValueLabel = new Label("0", Label.LEFT);
    Label instructionValueLabel = new Label("NONE", Label.LEFT);
    Label addressValueLabel = new Label("NULL", Label.LEFT);
    Label pageFaultValueLabel = new Label("NO", Label.LEFT);
    Label virtualPageValueLabel = new Label("x", Label.LEFT);
    Label physicalPageValueLabel = new Label("0", Label.LEFT);
    Label RValueLabel = new Label("0", Label.LEFT);
    Label MValueLabel = new Label("0", Label.LEFT);
    Label inMemTimeValueLabel = new Label("0", Label.LEFT);
    Label lastTouchTimeValueLabel = new Label("0", Label.LEFT);
    Label lowValueLabel = new Label("0", Label.LEFT);
    Label highValueLabel = new Label("0", Label.LEFT);

    Label segmentLabel = new Label("", Label.LEFT);
    TextArea pageInSegmentArea = new TextArea("", 6, 15, TextArea.SCROLLBARS_NONE);

    public ControlPanel() { super(); }
    public ControlPanel(String title) { super(title); }

    public void init(Kernel useKernel, String commands, String config) {
        kernel = useKernel;
        kernel.setControlPanel(this);

        setLayout(null);
        setBackground(Color.white);
        setForeground(Color.black);
        resize(635, 545);
        setFont(new Font("Courier", 0, 12));

        configureControlButton(runButton, 0, 25);
        configureControlButton(stepButton, 70, 25);
        configureControlButton(resetButton, 140, 25);
        configureControlButton(exitButton, 210, 25);

        int colX[] = {0, 140};
        for (int i = 0; i < PAGE_COUNT; i++) {
            int col = (i < 32) ? 0 : 1;
            int row = (i % 32);
            Button b = new Button("page " + i);
            b.setForeground(Color.magenta);
            b.setBackground(Color.lightGray);
            b.setBounds(colX[col], (row + 2) * 15 + 25, 70, 15);
            pageButtons[i] = b;
            add(b);
        }

        for (int i = 0; i < PAGE_COUNT; i++) {
            int x = (i < 32) ? 70 : 210;
            int row = (i % 32);
            Label l = new Label("", Label.CENTER);
            l.setForeground(Color.red);
            l.setFont(new Font("Courier", Font.PLAIN, 10));
            l.setBounds(x, (row + 2) * 15 + 25, 60, 15);
            physicalLabels[i] = l;
            add(l);
        }

        statusValueLabel.setBounds(345, 0 + 25, 100, 15);
        add(statusValueLabel);

        timeValueLabel.setBounds(345, 15 + 25, 100, 15);
        add(timeValueLabel);

        instructionValueLabel.setBounds(385, 45 + 25, 100, 15);
        add(instructionValueLabel);

        addressValueLabel.setBounds(385, 60 + 25, 230, 15);
        add(addressValueLabel);

        pageFaultValueLabel.setBounds(385, 90 + 25, 100, 15);
        add(pageFaultValueLabel);

        virtualPageValueLabel.setBounds(395, 120 + 25, 200, 15);
        add(virtualPageValueLabel);

        physicalPageValueLabel.setBounds(395, 135 + 25, 200, 15);
        add(physicalPageValueLabel);

        RValueLabel.setBounds(395, 150 + 25, 200, 15);
        add(RValueLabel);

        MValueLabel.setBounds(395, 165 + 25, 200, 15);
        add(MValueLabel);

        inMemTimeValueLabel.setBounds(395, 180 + 25, 200, 15);
        add(inMemTimeValueLabel);

        lastTouchTimeValueLabel.setBounds(395, 195 + 25, 200, 15);
        add(lastTouchTimeValueLabel);

        lowValueLabel.setBounds(395, 210 + 25, 230, 15);
        add(lowValueLabel);

        highValueLabel.setBounds(395, 225 + 25, 230, 15);
        add(highValueLabel);

        addFixedLabel("status:", 285, 0 + 25, 65, 15);
        addFixedLabel("time:", 285, 15 + 25, 50, 15);
        addFixedLabel("instruction:", 285, 45 + 25, 100, 15);
        addFixedLabel("address:", 285, 60 + 25, 85, 15);
        addFixedLabel("page fault:", 285, 90 + 25, 100, 15);
        addFixedLabel("virtual page:", 285, 120 + 25, 110, 15);
        addFixedLabel("physical page:", 285, 135 + 25, 110, 15);
        addFixedLabel("R:", 285, 150 + 25, 110, 15);
        addFixedLabel("M:", 285, 165 + 25, 110, 15);
        addFixedLabel("inMemTime:", 285, 180 + 25, 110, 15);
        addFixedLabel("lastTouchTime:", 285, 195 + 25, 110, 15);
        addFixedLabel("low:", 285, 210 + 25, 110, 15);
        addFixedLabel("high:", 285, 225 + 25, 110, 15);

        segmentLabel.setBounds(395, 240 + 25, 150, 15);
        segmentLabel.setFont(new Font("Courier", Font.BOLD, 14));
        add(segmentLabel);

        pageInSegmentArea.setBounds(395, 260 + 25, 150, 90);
        pageInSegmentArea.setFont(new Font("Courier", Font.PLAIN, 12));
        pageInSegmentArea.setEditable(false);
        pageInSegmentArea.setBackground(Color.white);
        add(pageInSegmentArea);

        Color[] segmentColors = {
            new Color(255, 200, 200),
            new Color(200, 255, 200),
            new Color(200, 200, 255),
            new Color(255, 255, 200),
            new Color(200, 255, 255)
        };

        for (int i = 0; i < PAGE_COUNT; i++) {
            int seg = findSegmentForPage(i);
            Color c = (seg >= 0 && seg < segmentColors.length) ? segmentColors[seg] : Color.lightGray;
            pageButtons[i].setBackground(c);
        }

        kernel.init(commands, config);

        show();
    }

    private void configureControlButton(Button b, int x, int y) {
        b.setForeground(Color.blue);
        b.setBackground(Color.lightGray);
        b.setBounds(x, y, 70, 15);
        add(b);
    }

    private void addFixedLabel(String text, int x, int y, int w, int h) {
        Label l = new Label(text, Label.LEFT);
        l.setBounds(x, y, w, h);
        add(l);
    }

    private int findSegmentForPage(int pageNum) {
        for (int s = 0; s < Kernel.SEGMENTS.length; s++) {
            for (int p = 0; p < Kernel.SEGMENTS[s].length; p++) {
                if (Kernel.SEGMENTS[s][p] == pageNum) return s;
            }
        }
        return -1;
    }

    public void paintPage(Page page) {
        if (page == null) return;
        virtualPageValueLabel.setText(Integer.toString(page.id));
        physicalPageValueLabel.setText(Integer.toString(page.physical));
        RValueLabel.setText(Integer.toString(page.R));
        MValueLabel.setText(Integer.toString(page.M));
        inMemTimeValueLabel.setText(Integer.toString(page.inMemTime));
        lastTouchTimeValueLabel.setText(Integer.toString(page.lastTouchTime));
        lowValueLabel.setText(Long.toString(page.low, Kernel.addressradix));
        highValueLabel.setText(Long.toString(page.high, Kernel.addressradix));

        int segnum = kernel.getSegmentForPage(page.id);
        if (segnum != -1) {
            segmentLabel.setText("Segmento: S" + (segnum + 1));
        } else {
            segmentLabel.setText("Sin segmento");
        }

        showPage(page.id);
    }

    public void paintPageOnly(Page page) {
        if (page == null) return;
        virtualPageValueLabel.setText(Integer.toString(page.id));
        physicalPageValueLabel.setText(Integer.toString(page.physical));
        RValueLabel.setText(Integer.toString(page.R));
        MValueLabel.setText(Integer.toString(page.M));
        inMemTimeValueLabel.setText(Integer.toString(page.inMemTime));
        lastTouchTimeValueLabel.setText(Integer.toString(page.lastTouchTime));
        lowValueLabel.setText(Long.toString(page.low, Kernel.addressradix));
        highValueLabel.setText(Long.toString(page.high, Kernel.addressradix));

        int segnum = kernel.getSegmentForPage(page.id);
        if (segnum != -1) {
            segmentLabel.setText("Segmento: S" + (segnum + 1));
        } else {
            segmentLabel.setText("Sin segmento");
        }
        // Do not change pageInSegmentArea here
    }

    public void setStatus(String status) {
        statusValueLabel.setText(status);
    }

    public void addPhysicalPage(int pageNum, int physicalPage) {
        if (physicalPage >= 0 && physicalPage < physicalLabels.length) {
            physicalLabels[physicalPage].setText("page " + pageNum);
        }
    }

    public void removePhysicalPage(int physicalPage) {
        if (physicalPage >= 0 && physicalPage < physicalLabels.length) {
            physicalLabels[physicalPage].setText(null);
        }
    }

    public void showPagesInRange(int start, int end) {
        if (start < 0 || end < start) {
            pageInSegmentArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int p = start; p <= end; p++) {
            sb.append("pagina ").append(p);
            if (p < end) sb.append(System.getProperty("line.separator"));
        }
        pageInSegmentArea.setText(sb.toString());
    }

    public void showPage(int p) {
        pageInSegmentArea.setText("Pagina: " + p);
    }

    public void clearPageList() {
        pageInSegmentArea.setText("");
    }

    public boolean action(Event e, Object arg) {
        Object target = e.target;

        if (target == runButton) {
            setStatus("RUN");
            runButton.disable();
            stepButton.disable();
            resetButton.disable();
            try {
                kernel.run();
            } catch (Throwable t) { }
            setStatus("STOP");
            resetButton.enable();
            return true;
        } else if (target == stepButton) {
            setStatus("STEP");
            kernel.step();
            if (kernel.runcycles == kernel.runs) {
                stepButton.disable();
                runButton.disable();
            }
            setStatus("STOP");
            return true;
        } else if (target == resetButton) {
            kernel.reset();
            runButton.enable();
            stepButton.enable();
            return true;
        } else if (target == exitButton) {
            System.exit(0);
            return true;
        } else {
            for (int i = 0; i < pageButtons.length; i++) {
                if (target == pageButtons[i]) {
                    kernel.getPage(i);
                    return true;
                }
            }
        }
        return false;
    }
}