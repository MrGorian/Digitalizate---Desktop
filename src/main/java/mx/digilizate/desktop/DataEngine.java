package mx.digilizate.desktop;
import java.util.*;
public final class DataEngine {
    private DataEngine() {
    }
    // Lista doblemente enlazada.
    public static final class Chain<T> implements Iterable<T> {
        private static final class Node<T> {
            T dato;
            Node<T> siguiente,anterior;
            Node(T v) {
                dato=v;
            }
        }
        private Node<T> primero,ultimo;
        private int size;
        public int size() {
            return size;
        }
        public void add(T v) {
            Node<T> n=new Node<>(v);
            n.anterior=ultimo;
            if(ultimo==null)primero=n;
            else ultimo.siguiente=n;
            ultimo=n;
            size++;
        }
        public T removeFirst() {
            if(primero==null)throw new NoSuchElementException();
            return unlink(primero);
        }
        public T removeLast() {
            if(ultimo==null)throw new NoSuchElementException();
            return unlink(ultimo);
        }
        private T unlink(Node<T> n) {
            if(n.anterior==null)primero=n.siguiente;
            else n.anterior.siguiente=n.siguiente;
            if(n.siguiente==null)ultimo=n.anterior;
            else n.siguiente.anterior=n.anterior;
            size--;
            return n.dato;
        }
        public Iterator<T> iterator() {
            return new Iterator<>() {
                Node<T> n=primero;
                public boolean hasNext() {
                    return n!=null;
                }
                public T next() {
                    if(n==null)throw new NoSuchElementException();
                    T v=n.dato;
                    n=n.siguiente;
                    return v;
                }
            }
            ;
        }
        public List<T> snapshot() {
            List<T> a=new ArrayList<>();
            for(T v:this)a.add(v);
            return a;
        }
    }
    // Pila LIFO.
    public static final class Stack<T> {
        private final Chain<T> lista=new Chain<>();
        public void push(T v) {
            lista.add(v);
        }
        public T pop() {
            return lista.removeLast();
        }
        public int size() {
            return lista.size();
        }
    }
    // Cola FIFO.
    public static final class Queue<T> {
        private final Chain<T> lista=new Chain<>();
        public void enqueue(T v) {
            lista.add(v);
        }
        public T dequeue() {
            return lista.removeFirst();
        }
        public int size() {
            return lista.size();
        }
    }
    // Ordenamiento por división y mezcla de mitades.
    public static <T> void mergeSort(List<T> datos,Comparator<T> comparador) {
        if(datos.size()<2)return;
        int mitad=datos.size()/2;
        List<T> izquierda=new ArrayList<>(datos.subList(0,mitad)),derecha=new ArrayList<>(datos.subList(mitad,datos.size()));
        mergeSort(izquierda,comparador);
        mergeSort(derecha,comparador);
        int i=0,j=0,posicion=0;
        while(i<izquierda.size() && j<derecha.size()) {
            if(comparador.compare(izquierda.get(i),derecha.get(j))<=0) {
                datos.set(posicion,izquierda.get(i));
                i++;
            }
            else {
                datos.set(posicion,derecha.get(j));
                j++;
            }
            posicion++;
        }
        while(i<izquierda.size()) {
            datos.set(posicion,izquierda.get(i));
            i++;
            posicion++;
        }
        while(j<derecha.size()) {
            datos.set(posicion,derecha.get(j));
            j++;
            posicion++;
        }
    }
    // Búsqueda sobre datos previamente ordenados.
    public static <T> int binarySearch(List<T> datos,T objetivo,Comparator<T> comparador) {
        int izquierda=0,derecha=datos.size()-1;
        while(izquierda<=derecha) {
            int mitad=izquierda+(derecha-izquierda)/2,resultado=comparador.compare(datos.get(mitad),objetivo);
            if(resultado==0)return mitad;
            if(resultado<0)izquierda=mitad+1;
            else derecha=mitad-1;
        }
        return -1;
    }
    public static <T> List<T> linearSearch(Iterable<T> a,java.util.function.Predicate<T> p) {
        List<T> out=new ArrayList<>();
        for(T v:a)if(p.test(v))out.add(v);
        return out;
    }
}
