package mx.digilizate.desktop;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
public final class Registry {
    public record Worker(String code,String name,String surname,boolean active) implements Serializable {
    }
    public record Event(String code,String time,String type) implements Serializable {
    }
    private record State(List<Worker> workers,List<Event> events) implements Serializable {
    }
    private DataEngine.Chain<Worker> workers=new DataEngine.Chain<>();
    private DataEngine.Chain<Event> events=new DataEngine.Chain<>();
    private final DataEngine.Stack<State> undo=new DataEngine.Stack<>();
    private final Path file;
    public Registry(Path file)throws IOException,ClassNotFoundException {
        this.file=file;
        if(Files.exists(file))try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(file))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("maxdepth=20;maxrefs=100000;java.base/*;mx.digilizate.desktop.Registry$*;!*"));
            restore((State)in.readObject());
        }
    }
    private State state() {
        return new State(workers.snapshot(),events.snapshot());
    }
    private void restore(State s) {
        workers=new DataEngine.Chain<>();
        events=new DataEngine.Chain<>();
        for(Worker worker:s.workers) {
            workers.add(worker);
        }
        for(Event event:s.events) {
            events.add(event);
        }
    }
    private void save()throws IOException {
        Path dir=file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp=Files.createTempFile(dir,"dgz", ".tmp");
        try {
            try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(state());
            }
            Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);
        }
        finally {
            Files.deleteIfExists(tmp);
        }
    }
    private void change(State next,boolean reversible)throws IOException {
        State old=state();
        restore(next);
        try {
            save();
        }
        catch(IOException e) {
            restore(old);
            throw e;
        }
        if(reversible)undo.push(old);
    }
    public List<Worker> workers() {
        return workers.snapshot();
    }
    public List<Event> events() {
        return events.snapshot();
    }
    public Worker find(String code) {
        List<Worker> a=workers();
        DataEngine.mergeSort(a,Comparator.comparing(Worker::code));
        int i=DataEngine.binarySearch(a,new Worker(code.trim().toUpperCase(Locale.ROOT),"","",true),Comparator.comparing(Worker::code));
        if(i<0) {
            return null;
        }
        return a.get(i);
    }
    public void put(String original,Worker input)throws IOException {
        String code=input.code.trim().toUpperCase(Locale.ROOT),name=input.name.trim(),surname=input.surname.trim();
        if(!code.matches("[A-Z0-9_-]{1,30}")||name.isEmpty()||surname.isEmpty()||name.length()>100||surname.length()>100)throw new IllegalArgumentException("Código, nombre y apellidos válidos obligatorios.");
        if(original!=null&&find(original)==null)throw new IllegalArgumentException("Registro inexistente.");
        if(original!=null&&!original.equals(code))throw new IllegalArgumentException("El código no se puede cambiar.");
        if(original==null&&find(code)!=null)throw new IllegalArgumentException("Código duplicado.");
        List<Worker> a=workers();
        for(int i=a.size()-1;i>=0;i--) {
            if(a.get(i).code.equals(code)) {
                a.remove(i);
            }
        }
        a.add(new Worker(code,name,surname,input.active));
        change(new State(a,events()),true);
    }
    public void delete(String code)throws IOException {
        if(find(code)==null)throw new IllegalArgumentException("Registro inexistente.");
        for(Event event:events()) {
            if(event.code.equals(code)) {
                throw new IllegalArgumentException("No se puede eliminar un trabajador con asistencias.");
            }
        }
        List<Worker>a=workers();
        for(int i=a.size()-1;i>=0;i--) {
            if(a.get(i).code.equals(code)) {
                a.remove(i);
            }
        }
        change(new State(a,events()),true);
    }
    public void undo()throws IOException {
        if(undo.size()==0)throw new IllegalArgumentException("No hay cambios pendientes de deshacer.");
        State old=undo.pop();
        try {
            change(new State(old.workers,events()),false);
        }
        catch(IOException e) {
            undo.push(old);
            throw e;
        }
    }
    public void attendance(String code)throws IOException {
        Worker w=find(code);
        if(w==null||!w.active)throw new IllegalArgumentException("Trabajador inexistente o inactivo.");
        List<Event>a=events();
        String type="ENTRADA";
        LocalDateTime now=LocalDateTime.now();
        for(int i=a.size()-1;i>=0;i--) {
            Event e=a.get(i);
            if(e.code.equals(w.code)) {
                LocalDateTime last=LocalDateTime.parse(e.time);
                if(!now.isBefore(last)&&Duration.between(last,now).getSeconds()<10)throw new IllegalArgumentException("Registro duplicado reciente.");
                if(last.toLocalDate().equals(now.toLocalDate())) {
                    if(e.type.equals("ENTRADA")) {
                        type="SALIDA";
                    }
                    else {
                        type="ENTRADA";
                    }
                }
                break;
            }
        }
        a.add(new Event(w.code,now.toString(),type));
        change(new State(workers(),a),false);
        while(undo.size()>0)undo.pop();
    }
}
