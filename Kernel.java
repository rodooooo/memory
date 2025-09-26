import java.lang.Thread;
import java.io.*;
import java.util.*;

public class Kernel extends Thread {
    // The number of virtual pages must be fixed at 63 due to dependencies in the GUI
    private static int virtPageNum = 63;

    // SEGMENTS: define distribution of pages by segment
    public static final int[][] SEGMENTS = {
        {0,1,2},
        {3,4,5,6,7,8},
        {9,10,11,12,13,14},
        {15,16,17,18,19,20,21,22},
        {23,24,25,26,27,28,29,30,31}
    };

    private String output = null;
    private static final String lineSeparator = System.getProperty("line.separator");
    private String command_file;
    private String config_file;
    private ControlPanel controlPanel;
    private Vector memVector = new Vector();
    private Vector instructVector = new Vector();
    private String status;
    private boolean doStdoutLog = false;
    private boolean doFileLog = false;
    public int runs;
    public int runcycles;
    public long block = (int) Math.pow(2,12);
    public static byte addressradix = 10;
    public int currentSegment = -1;

    public void init(String commands, String config) {
        // (Implementation identical to the existing init in your repo; ensure this file
        // contains the init method. If you already have an init variant, keep it.)
        System.out.println("Kernel.init: commands=" + commands + " config=" + config);

        File f = new File(commands);
        command_file = commands;
        config_file = config;
        String line;
        String tmp = null;
        String command = "";
        byte R = 0;
        byte M = 0;
        int i = 0;
        int j = 0;
        int id = 0;
        int physical = 0;
        int physical_count = 0;
        int inMemTime = 0;
        int lastTouchTime = 0;
        int map_count = 0;
        double power = 14;
        long high = 0;
        long low = 0;
        long addr = 0;
        long address_limit = (block * virtPageNum+1)-1;

        if ( config != null ) {
            f = new File ( config );
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("numpages")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            virtPageNum = Common.s2i(st.nextToken()) - 1;
                            if ( virtPageNum < 2 || virtPageNum > 63 ) {
                                System.out.println("MemoryManagement: numpages out of bounds.");
                                System.exit(-1);
                            }
                            address_limit = (block * virtPageNum+1)-1;
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }
            for (i = 0; i <= virtPageNum; i++) {
                high = (block * (i + 1))-1;
                low = block * i;
                memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
            }
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("memset")) {
                        StringTokenizer st = new StringTokenizer(line);
                        st.nextToken();
                        while (st.hasMoreTokens()) {
                            id = Common.s2i(st.nextToken());
                            tmp = st.nextToken();
                            if (tmp.startsWith("x")) {
                                physical = -1;
                            } else {
                                physical = Common.s2i(tmp);
                            }
                            if ((0 > id || id > virtPageNum) || (-1 > physical || physical > ((virtPageNum - 1) / 2))) {
                                System.out.println("MemoryManagement: Invalid page value in " + config);
                                System.exit(-1);
                            }
                            R = Common.s2b(st.nextToken());
                            if (R < 0 || R > 1) {
                                System.out.println("MemoryManagement: Invalid R value in " + config);
                                System.exit(-1);
                            }
                            M = Common.s2b(st.nextToken());
                            if (M < 0 || M > 1) {
                                System.out.println("MemoryManagement: Invalid M value in " + config);
                                System.exit(-1);
                            }
                            inMemTime = Common.s2i(st.nextToken());
                            if (inMemTime < 0) {
                                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                                System.exit(-1);
                            }
                            lastTouchTime = Common.s2i(st.nextToken());
                            if (lastTouchTime < 0) {
                                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                                System.exit(-1);
                            }
                            Page page = (Page) memVector.elementAt(id);
                            page.physical = physical;
                            page.R = R;
                            page.M = M;
                            page.inMemTime = inMemTime;
                            page.lastTouchTime = lastTouchTime;
                        }
                    }
                    if (line.startsWith("enable_logging")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            if ( st.nextToken().startsWith( "true" ) ) {
                                doStdoutLog = true;
                            }
                        }
                    }
                    if (line.startsWith("log_file")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                        }
                        if ( tmp.startsWith( "log_file" ) ) {
                            doFileLog = false;
                            output = "tracefile";
                        } else {
                            doFileLog = true;
                            doStdoutLog = false;
                            output = tmp;
                        }
                    }
                    if (line.startsWith("pagesize")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            tmp = st.nextToken();
                            if ( tmp.startsWith( "power" ) ) {
                                power = (double) Integer.parseInt(st.nextToken());
                                block = (int) Math.pow(2,power);
                            } else {
                                block = Long.parseLong(tmp,10);
                            }
                            address_limit = (block * virtPageNum+1)-1;
                        }
                        if ( block < 64 || block > Math.pow(2,26)) {
                            System.out.println("MemoryManagement: pagesize is out of bounds");
                            System.exit(-1);
                        }
                        for (i = 0; i <= virtPageNum; i++) {
                            Page page = (Page) memVector.elementAt(i);
                            page.high = (block * (i + 1))-1;
                            page.low = block * i;
                        }
                    }
                    if (line.startsWith("addressradix")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            tmp = st.nextToken();
                            addressradix = Byte.parseByte(tmp);
                            if ( addressradix < 0 || addressradix > 20 ) {
                                System.out.println("MemoryManagement: addressradix out of bounds.");
                                System.exit(-1);
                            }
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }
        }
        f = new File ( commands );
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            while ((line = in.readLine()) != null) {
                if (line.startsWith("READ") || line.startsWith("WRITE")) {
                    if (line.startsWith("READ")) command = "READ";
                    if (line.startsWith("WRITE")) command = "WRITE";
                    StringTokenizer st = new StringTokenizer(line);
                    tmp = st.nextToken();
                    tmp = st.nextToken();
                    if (tmp.startsWith("random")) {
                        instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
                    } else {
                        if (tmp.indexOf('-') > -1 && !tmp.startsWith("bin") && !tmp.startsWith("oct") && !tmp.startsWith("hex")) {
                            String[] parts = tmp.split("-");
                            long start = Long.parseLong(parts[0],16);
                            long end = Long.parseLong(parts[1],16);
                            instructVector.addElement(new Instruction(command,start,end));
                        } else {
                            if ( tmp.startsWith( "bin" ) || tmp.startsWith( "oct" ) || tmp.startsWith( "hex" ) ) {
                                String value = st.nextToken();
                                if (value.contains("-")) {
                                    String[] parts = value.split("-");
                                    long start, end;
                                    if (tmp.startsWith("bin")) {
                                        start = Long.parseLong(parts[0],2);
                                        end = Long.parseLong(parts[1],2);
                                    } else if (tmp.startsWith("oct")) {
                                        start = Long.parseLong(parts[0],8);
                                        end = Long.parseLong(parts[1],8);
                                    } else {
                                        start = Long.parseLong(parts[0],16);
                                        end = Long.parseLong(parts[1],16);
                                    }
                                    instructVector.addElement(new Instruction(command, start, end));
                                } else {
                                    if (tmp.startsWith("bin")) {
                                        addr = Long.parseLong(value,2);
                                    } else if (tmp.startsWith("oct")) {
                                        addr = Long.parseLong(value,8);
                                    } else {
                                        addr = Long.parseLong(value,16);
                                    }
                                    instructVector.addElement(new Instruction(command, addr));
                                }
                            } else {
                                if (tmp.contains("-")) {
                                    String[] parts = tmp.split("-");
                                    long start = Long.parseLong(parts[0]);
                                    long end = Long.parseLong(parts[1]);
                                    instructVector.addElement(new Instruction(command, start, end));
                                } else {
                                    addr = Long.parseLong(tmp);
                                    instructVector.addElement(new Instruction(command, addr));
                                }
                            }
                        }
                    }
                }
            }
            in.close();
        } catch (IOException e) { /* Handle exceptions */ }
        runcycles = instructVector.size();
        if ( runcycles < 1 ) {
            System.out.println("MemoryManagement: no instructions present for execution.");
        }
        if ( doFileLog ) {
            File trace = new File(output);
            trace.delete();
        }
        runs = 0;
        for (i = 0; i < virtPageNum; i++) {
            Page page = (Page) memVector.elementAt(i);
            if ( page.physical != -1 ) {
                map_count++;
            }
            for (j = 0; j < virtPageNum; j++) {
                Page tmp_page = (Page) memVector.elementAt(j);
                if (tmp_page.physical == page.physical && page.physical >= 0) {
                    physical_count++;
                }
            }
            if (physical_count > 1) {
                System.out.println("MemoryManagement: Duplicate physical page's in " + config);
                System.exit(-1);
            }
            physical_count = 0;
        }
        if ( map_count < ( virtPageNum +1 ) / 2 ) {
            for (i = 0; i < virtPageNum; i++) {
                Page page = (Page) memVector.elementAt(i);
                if ( page.physical == -1 && map_count < ( virtPageNum + 1 ) / 2 ) {
                    page.physical = i;
                    map_count++;
                }
            }
        }
        for (i = 0; i < virtPageNum; i++) {
            Page page = (Page) memVector.elementAt(i);
            if (page.physical == -1) {
                if (controlPanel != null) controlPanel.removePhysicalPage( i );
            } else {
                if (controlPanel != null) controlPanel.addPhysicalPage( i , page.physical );
            }
        }
        for (i = 0; i < instructVector.size(); i++) {
            high = block * virtPageNum;
            Instruction instruct = ( Instruction ) instructVector.elementAt( i );
            if ( instruct.addr < 0 || instruct.addr > high ) {
                System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
                System.exit(-1);
            }
        }

        System.out.println("Kernel.init: memVector.size=" + (memVector==null?0:memVector.size())
            + " instructVector.size=" + (instructVector==null?0:instructVector.size()));
    }

    public void setControlPanel(ControlPanel newControlPanel) {
        controlPanel = newControlPanel;
    }

    public void getPage(int pageNum) {
        if (memVector == null || pageNum < 0 || pageNum >= memVector.size()) {
            if (controlPanel != null) {
                controlPanel.setStatus("No hay paginas cargadas");
                try {
                    if (controlPanel.virtualPageValueLabel != null) controlPanel.virtualPageValueLabel.setText("x");
                    if (controlPanel.physicalPageValueLabel != null) controlPanel.physicalPageValueLabel.setText("0");
                    if (controlPanel.RValueLabel != null) controlPanel.RValueLabel.setText("0");
                    if (controlPanel.MValueLabel != null) controlPanel.MValueLabel.setText("0");
                    if (controlPanel.inMemTimeValueLabel != null) controlPanel.inMemTimeValueLabel.setText("0");
                    if (controlPanel.lastTouchTimeValueLabel != null) controlPanel.lastTouchTimeValueLabel.setText("0");
                    if (controlPanel.lowValueLabel != null) controlPanel.lowValueLabel.setText("0");
                    if (controlPanel.highValueLabel != null) controlPanel.highValueLabel.setText("0");
                    if (controlPanel.segmentLabel != null) controlPanel.segmentLabel.setText("");
                } catch (Exception e) {
                    // protect against different ControlPanel versions
                }
            }
            return;
        }

        Page page = (Page) memVector.elementAt(pageNum);
        if (controlPanel != null) {
            try {
                controlPanel.paintPage(page);
            } catch (Exception e) {
                controlPanel.setStatus("Error al mostrar pagina");
            }
        }
    }

    public int getSegmentForPage(int pageNum) {
        for (int s = 0; s < SEGMENTS.length; s++) {
            for (int p = 0; p < SEGMENTS[s].length; p++) {
                if (SEGMENTS[s][p] == pageNum) return s;
            }
        }
        return -1;
    }

    public int pageForAddress(long addr) {
        if (block <= 0) return -1;
        return (int)(addr / block);
    }

    public String formatAddress(long addr) {
        int radix = (int) Kernel.addressradix;
        if (radix == 16) return Long.toHexString(addr);
        if (radix == 10) return Long.toString(addr, 10);
        if (radix == 8)  return Long.toString(addr, 8);
        if (radix == 2)  return Long.toString(addr, 2);
        return Long.toString(addr, 10);
    }

    private void printLogFile(String message) {
        String line;
        String temp = "";

        File trace = new File(output);
        if (trace.exists()) {
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(output));
                while ((line = in.readLine()) != null) {
                    temp = temp + line + lineSeparator;
                }
                in.close();
            } catch ( IOException e ) { /* Do nothing */ }
        }
        try {
            PrintStream out = new PrintStream(new FileOutputStream(output));
            out.print(temp);
            out.print(message);
            out.close();
        } catch (IOException e) { /* Do nothing */ }
    }

    public void run() {
        step();
        while (runs != runcycles) {
            try {
                Thread.sleep(2000);
            } catch(InterruptedException e) { /* Do nothing */ }
            step();
        }
    }

    public void step() {
        if (instructVector == null || instructVector.size() == 0 || runs >= instructVector.size()) {
            if (controlPanel != null) {
                controlPanel.statusValueLabel.setText("ERROR: No hay instrucciones para ejecutar.");
                controlPanel.clearPageList();
            }
            return;
        }

        Instruction instruct = (Instruction) instructVector.elementAt(runs);
        int segStart = -1, segEnd = -1, pageStart = -1, pageEnd = -1;
        boolean errorSegment = false;
        if (controlPanel != null) controlPanel.instructionValueLabel.setText(instruct.inst);

        String basePrefix = "";
        int radix = addressradix;
        switch (radix) {
            case 16: basePrefix = "0x"; break;
            case 8: basePrefix = "0"; break;
            case 2: basePrefix = "0b"; break;
            default: basePrefix = ""; break;
        }

        if (instruct.isRange()) {
            if (controlPanel != null) controlPanel.addressValueLabel.setText(Long.toString(instruct.addr, addressradix) + " - " + Long.toString(instruct.endAddr, addressradix));
            pageStart = Virtual2Physical.pageNum(instruct.addr, virtPageNum, block);
            pageEnd = Virtual2Physical.pageNum(instruct.endAddr, virtPageNum, block);

            if (controlPanel != null) controlPanel.showPagesInRange(pageStart, pageEnd);

            segStart = getSegmentForPage(pageStart);
            segEnd = getSegmentForPage(pageEnd);
            if (segStart == -1 || segEnd == -1) {
                errorSegment = true;
            } else {
                if (segStart != segEnd) errorSegment = true;
                else currentSegment = segStart;
            }

            String pageStartStr = basePrefix + Long.toString(pageStart, radix);
            String pageEndStr = basePrefix + Long.toString(pageEnd, radix);
            if (controlPanel != null && controlPanel.virtualPageValueLabel != null)
                controlPanel.virtualPageValueLabel.setText(pageStartStr + " - " + pageEndStr);

        } else {
            if (controlPanel != null) controlPanel.addressValueLabel.setText(Long.toString(instruct.addr, addressradix));
            pageStart = Virtual2Physical.pageNum(instruct.addr, virtPageNum, block);
            segStart = getSegmentForPage(pageStart);

            String pageStr = basePrefix + Long.toString(pageStart, radix);
            if (controlPanel != null && controlPanel.virtualPageValueLabel != null)
                controlPanel.virtualPageValueLabel.setText(pageStr);

            if (controlPanel != null) controlPanel.showPage(pageStart);

            if (segStart == -1) {
                errorSegment = true;
            } else {
                currentSegment = segStart;
            }
        }

        if (errorSegment) {
            if (controlPanel != null) {
                controlPanel.pageFaultValueLabel.setText("ERROR SEGMENTO");
                if (controlPanel.segmentLabel != null) controlPanel.segmentLabel.setText("Error: Direccion fuera de segmento");
                controlPanel.clearPageList();
            }
            runs++;
            return;
        } else {
            if (controlPanel != null && controlPanel.segmentLabel != null) controlPanel.segmentLabel.setText("S" + (currentSegment+1));
        }

        if (instruct.isRange()) {
            boolean pageFaultOccurred = false;
            for (int p = pageStart; p <= pageEnd; p++) {
                Page page = (Page) memVector.elementAt(p);
                boolean wasNotPresent = (page.physical == -1);
                if (wasNotPresent) {
                    PageFault.replacePage(memVector, virtPageNum, p, controlPanel);
                    if (controlPanel != null) controlPanel.pageFaultValueLabel.setText("YES");
                    pageFaultOccurred = true;
                } else {
                    if (controlPanel != null) controlPanel.pageFaultValueLabel.setText("NO");
                }
                page.R = 1;
                if ("WRITE".equals(instruct.inst)) page.M = 1;
                page.lastTouchTime = runs * 10;
            }

            Page first = (Page) memVector.elementAt(pageStart);
            if (controlPanel != null) controlPanel.paintPageOnly(first);

            if (controlPanel != null) controlPanel.pageFaultValueLabel.setText(pageFaultOccurred ? "YES" : "NO");

            String addrRangeStr = formatAddress(instruct.addr) + " - " + formatAddress(instruct.endAddr);
            String status = pageFaultOccurred ? "page fault" : "okay";
            String messageRange = instruct.inst + " " + addrRangeStr + " ... " + status + " (pages " + pageStart + "-" + pageEnd + ")" + lineSeparator;
            if (doStdoutLog) System.out.print(messageRange);
            if (doFileLog && output != null) printLogFile(messageRange);

        } else {
            Page page = (Page) memVector.elementAt(pageStart);
            boolean wasNotPresent = (page.physical == -1);
            if (wasNotPresent) {
                PageFault.replacePage(memVector, virtPageNum, pageStart, controlPanel);
                if (controlPanel != null) controlPanel.pageFaultValueLabel.setText("YES");
            } else {
                if (controlPanel != null) controlPanel.pageFaultValueLabel.setText("NO");
            }
            page.R = 1;
            if ("WRITE".equals(instruct.inst)) page.M = 1;
            page.lastTouchTime = runs * 10;
            getPage(pageStart);

            String addrStr = formatAddress(instruct.addr);
            String status = wasNotPresent ? "page fault" : "okay";
            String message = instruct.inst + " " + addrStr + " ... " + status + " (page " + pageStart + ")" + lineSeparator;
            if (doStdoutLog) System.out.print(message);
            if (doFileLog && output != null) printLogFile(message);
        }

        runs++;
        if (controlPanel != null && controlPanel.timeValueLabel != null)
            controlPanel.timeValueLabel.setText(Integer.toString(runs*10) + " (ns)");
    }

    public void reset() {
        memVector.removeAllElements();
        instructVector.removeAllElements();
        if (controlPanel != null) controlPanel.statusValueLabel.setText("STOP");
        if (controlPanel != null) controlPanel.timeValueLabel.setText("0");
        if (controlPanel != null) controlPanel.instructionValueLabel.setText("NONE");
        if (controlPanel != null) controlPanel.addressValueLabel.setText("NULL");
        if (controlPanel != null) controlPanel.pageFaultValueLabel.setText("NO");
        if (controlPanel != null) controlPanel.virtualPageValueLabel.setText("x");
        if (controlPanel != null) controlPanel.physicalPageValueLabel.setText("0");
        if (controlPanel != null) controlPanel.RValueLabel.setText("0");
        if (controlPanel != null) controlPanel.MValueLabel.setText("0");
        if (controlPanel != null) controlPanel.inMemTimeValueLabel.setText("0");
        if (controlPanel != null) controlPanel.lastTouchTimeValueLabel.setText("0");
        if (controlPanel != null) controlPanel.lowValueLabel.setText("0");
        if (controlPanel != null) controlPanel.highValueLabel.setText("0");
        if (controlPanel != null && controlPanel.segmentLabel != null) controlPanel.segmentLabel.setText("");
        init(command_file, config_file);
    }
}