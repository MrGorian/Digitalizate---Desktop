package mx.digilizate.desktop;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
public final class Desktop extends JFrame {
    private final Registry registry;
    private final DefaultTableModel model=new DefaultTableModel(new String[] {
        "Código","Nombre","Apellidos","Estado"
    }
    ,0) {
        public boolean isCellEditable(int r,int c) {
            return false;
        }
    }
    ;
    private final JTable table=new JTable(model);
    private final JTextField query=new JTextField(20);
    private final JLabel status=new JLabel();
    private final JTextArea reports=new JTextArea();
    private final DataEngine.Queue<String> jobs=new DataEngine.Queue<>();
    private final Color navy=new Color(11,26,51);
    public Desktop(Registry r) {
        super("Digitalízate Desktop | Administración");
        registry=r;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150,720);
        setMinimumSize(new Dimension(850,550));
        setLocationRelativeTo(null);
        JPanel header=new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(BorderFactory.createEmptyBorder(18,24,18,24));
        JLabel brand=new JLabel("DIGITALÍZATE  /  DESKTOP");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif",Font.BOLD,24));
        header.add(brand);
        add(header,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        tabs.setFont(new Font("SansSerif",Font.BOLD,15));
        add(tabs);
        JPanel workers=new JPanel(new BorderLayout(8,8));
        workers.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT));
        tools.add(query);
        button(tools,"Buscar",()->refresh(DataEngine.linearSearch(registry.workers(),w->(w.code()+" "+w.name()+" "+w.surname()).toLowerCase(Locale.ROOT).contains(query.getText().trim().toLowerCase(Locale.ROOT)))));
        button(tools,"Código exacto",()-> {
            Registry.Worker w=registry.find(query.getText());refresh(w==null?List.of():List.of(w));
        }
        );
        button(tools,"Todos",this::refresh);
        button(tools,"Exportar CSV",this::exportWorkers);
        button(tools,"Ordenar",()-> {
            List<Registry.Worker> a=registry.workers();DataEngine.mergeSort(a,Comparator.comparing(Registry.Worker::surname,String.CASE_INSENSITIVE_ORDER).thenComparing(Registry.Worker::name));refresh(a);
        }
        );
        workers.add(tools,BorderLayout.NORTH);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setBackground(Color.WHITE);
        table.getTableHeader().setBackground(navy);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,14));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workers.add(new JScrollPane(table));
        JPanel actions=new JPanel();
        button(actions,"Registrar",()->form(null));
        button(actions,"Editar",()->form(selected()));
        button(actions,"Eliminar",()-> {
            Registry.Worker w=selected();if(JOptionPane.showConfirmDialog(this,"¿Eliminar "+w.code()+"?","Confirmar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                registry.delete(w.code());refresh();
            }
        }
        );
        button(actions,"Deshacer",()-> {
            registry.undo();refresh();
        }
        );
        button(actions,"Registrar asistencia",()-> {
            registry.attendance(selected().code());refresh();JOptionPane.showMessageDialog(this,"Asistencia registrada");
        }
        );
        workers.add(actions,BorderLayout.SOUTH);
        JPanel home=new JPanel(new BorderLayout(20,20));
        home.setBorder(BorderFactory.createEmptyBorder(30,30,30,30));
        JLabel welcome=new JLabel("Panel de administración");
        welcome.setFont(new Font("SansSerif",Font.BOLD,28));
        home.add(welcome,BorderLayout.NORTH);
        JPanel metrics=new JPanel(new GridLayout(1,3,20,20));
        JLabel total=new JLabel(),activeCount=new JLabel(),eventCount=new JLabel();
        for(JLabel metric:new JLabel[] {
            total,activeCount,eventCount
        }
        ) {
            metric.setOpaque(true);
            metric.setBackground(Color.WHITE);
            metric.setForeground(navy);
            metric.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
            metric.setFont(new Font("SansSerif",Font.BOLD,22));
            metrics.add(metric);
        }
        home.add(metrics,BorderLayout.CENTER);
        JPanel shortcuts=new JPanel();
        button(shortcuts,"Administrar trabajadores",()->tabs.setSelectedIndex(1));
        button(shortcuts,"Consultar asistencias",()->tabs.setSelectedIndex(3));
        home.add(shortcuts,BorderLayout.SOUTH);
        Runnable updateHome=()-> {
            total.setText("<html>Trabajadores<br>"+registry.workers().size()+"</html>");
            activeCount.setText("<html>Activos<br>"+countActive()+"</html>");
            eventCount.setText("<html>Asistencias<br>"+registry.events().size()+"</html>");
        }
        ;
        tabs.addTab("Inicio",home);
        tabs.addTab("Trabajadores",workers);
        tabs.addChangeListener(e->updateHome.run());
        updateHome.run();
        JPanel rp=new JPanel(new BorderLayout());
        reports.setEditable(false);
        reports.setFont(new Font("Monospaced",Font.PLAIN,14));
        rp.add(new JScrollPane(reports));
        JPanel rt=new JPanel();
        button(rt,"Solicitar reporte",()-> {
            jobs.enqueue("Resumen");status.setText("Tareas pendientes: "+jobs.size());
        }
        );
        button(rt,"Generar reporte",()-> {
            if(jobs.size()==0)throw new IllegalArgumentException("Cola vacía");jobs.dequeue();StringBuilder b=new StringBuilder("REPORTE DE ASISTENCIAS\n\nTrabajadores: "+registry.workers().size()+"\nEventos: "+registry.events().size()+"\n\n");for(Registry.Event e:registry.events())b.append(e.code()).append("  ").append(e.time()).append("  ").append(e.type()).append('\n');reports.setText(b.toString());status.setText("Tareas pendientes: "+jobs.size());
        }
        );
        rp.add(rt,BorderLayout.NORTH);
        tabs.addTab("Reportes",rp);
        JPanel history=new JPanel(new BorderLayout());
        JTextArea events=new JTextArea();
        events.setEditable(false);
        history.add(new JScrollPane(events));
        JPanel ht=new JPanel();
        button(ht,"Actualizar historial",()-> {
            StringBuilder b=new StringBuilder();for(Registry.Event e:registry.events())b.append(e.code()).append(" | ").append(e.time()).append(" | ").append(e.type()).append('\n');events.setText(b.toString());
        }
        );
        history.add(ht,BorderLayout.NORTH);
        tabs.addTab("Asistencias",history);
        add(status,BorderLayout.SOUTH);
        refresh();
    }
    private int countActive() {
        int count=0;
        for(Registry.Worker worker:registry.workers()) {
            if(worker.active()) {
                count++;
            }
        }
        return count;
    }
    private void exportWorkers() throws Exception {
        JFileChooser f=new JFileChooser();
        f.setSelectedFile(new java.io.File("trabajadores.csv"));
        if(f.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=f.getSelectedFile().toPath();
        if(Files.exists(path)&&JOptionPane.showConfirmDialog(this,"¿Sobrescribir archivo?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        StringBuilder b=new StringBuilder("codigo,nombre,apellidos,activo\n");
        for(Registry.Worker w:registry.workers())b.append(csv(w.code())).append(',').append(csv(w.name())).append(',').append(csv(w.surname())).append(',').append(w.active()).append('\n');
        Files.writeString(path,b.toString());
    }
    private static String csv(String s) {
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    private interface Action {
        void run()throws Exception;
    }
    private void button(JPanel p,String label,Action a) {
        JButton b=new JButton(label);
        b.setBackground(navy);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10,16,10,16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e-> {
            try {
                a.run();
            }
            catch(Exception ex) {
                JOptionPane.showMessageDialog(this,ex.getMessage(),"Operación no realizada",JOptionPane.WARNING_MESSAGE);
            }
        }
        );
        p.add(b);
    }
    private Registry.Worker selected() {
        int i=table.getSelectedRow();
        if(i<0)throw new IllegalArgumentException("Selecciona un trabajador.");
        return registry.find((String)model.getValueAt(i,0));
    }
    private void refresh() {
        refresh(registry.workers());
    }
    private void refresh(List<Registry.Worker> a) {
        model.setRowCount(0);
        for(Registry.Worker w:a)model.addRow(new Object[] {
            w.code(),w.name(),w.surname(),w.active()?"Activo":"Inactivo"
        }
        );
        status.setText("Registros visibles: "+a.size()+" | Total: "+registry.workers().size());
    }
    private void form(Registry.Worker w)throws Exception {
        JTextField c=new JTextField(w==null?"":w.code()),n=new JTextField(w==null?"":w.name()),s=new JTextField(w==null?"":w.surname());
        c.setEditable(w==null);
        JCheckBox active=new JCheckBox("Activo",w==null||w.active());
        JPanel p=new JPanel(new GridLayout(0,2,8,8));
        p.add(new JLabel("Código"));
        p.add(c);
        p.add(new JLabel("Nombre"));
        p.add(n);
        p.add(new JLabel("Apellidos"));
        p.add(s);
        p.add(active);
        if(JOptionPane.showConfirmDialog(this,p,"Trabajador",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            registry.put(w==null?null:w.code(),new Registry.Worker(c.getText(),n.getText(),s.getText(),active.isSelected()));
            refresh();
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()-> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());UIManager.put("Panel.background",new Color(240,244,249));UIManager.put("Label.font",new Font("SansSerif",Font.PLAIN,14));UIManager.put("Button.font",new Font("SansSerif",Font.BOLD,13));UIManager.put("Table.font",new Font("SansSerif",Font.PLAIN,14));UIManager.put("TabbedPane.selected",Color.WHITE);Path p=args.length>0?Path.of(args[0]):Path.of(System.getProperty("user.home"),"DigitalizateDesktop","datos.bin");new Desktop(new Registry(p)).setVisible(true);
            }
            catch(Exception e) {
                JOptionPane.showMessageDialog(null,e.toString());
            }
        }
        );
    }
}
