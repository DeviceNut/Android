package com.devicenut.pixelnutctrl;

@SuppressWarnings("unchecked")
class PCQueue<E>
{
    private int head;
    private int tail;
    private final int qcount;
    private final Object[] queue;

    PCQueue(int count)
    {
        qcount = count+1; // one slot still empty when queue is full
        queue = new Object[qcount];
        head = tail = 0;
    }

    boolean put(E e)
    {
        if (e == null) return false;

        int next = (tail + 1) % qcount;
        if (next == head) return false;

        queue[tail] = e;
        tail = next;
        return true;
    }

    boolean empty()
    {
        return head==tail;
    }
    void clear() { head=tail; }

    E peek()
    {
        if (empty()) return null;
        return (E) queue[head];
    }

    E get()
    {
        E e = peek();
        if (e != null) head = (head + 1) % qcount;
        return e;
    }
}
