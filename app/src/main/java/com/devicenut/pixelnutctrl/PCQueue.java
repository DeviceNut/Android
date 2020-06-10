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
        clear();
    }

    boolean empty()
    {
        return head==tail;
    }
    void clear() { head = tail = 0; }

    synchronized boolean put(E e)
    {
        if (e == null) return false;

        int next = (tail + 1) % qcount;
        if (next == head) return false;

        queue[tail] = e;
        tail = next;
        return true;
    }

    synchronized E peek()
    {
        if (empty()) return null;
        return (E) queue[head];
    }

    synchronized E get()
    {
        if (empty()) return null;
        E e = (E) queue[head];
        head = (head + 1) % qcount;
        return e;
    }
}
