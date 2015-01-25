package net.vidageek.crawler.queue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author jonasabreu
 * 
 */
final public class DelayedBlockingQueue implements BlockingQueue<Runnable> {

    //声明一个BlockQueue用来保存PageCrawlerExecutor，由于PageCrawlerExecutor
    //实现了Runnable接口采用接口反调的方法用Runnable接口指向PageCrawlerExecutor
    //BlockingQueue是线程安全的
	private final BlockingQueue<Runnable> queue;
    //延时时间，当延时了delayInMilliseconds时间后
    //消费者获取下一个对象
	private final int delayInMilliseconds;
    //记录队列上一次消费者取得Runnable进程的时间，和delayInMilliseconds一起控制队列的延时
    //两者起到每隔delayInMilliseconds的实现，访问一次网页
	private volatile long lastSuccesfullPop;

	public DelayedBlockingQueue(final int delayInMilliseconds) {
		this.delayInMilliseconds = delayInMilliseconds;
        //将BlockingQueue声明为LinkedBlockQueue，队列的类型为Runnable即进程对象
		queue = new LinkedBlockingQueue<Runnable>();
        //队列对象初始化时用系统的当前时间减去一个延时时间作为队列初始化时上一次成功取得对象时间
        //可以保证队列初始化完成后能够立即获取到第一个进程对象
		lastSuccesfullPop = System.currentTimeMillis() - delayInMilliseconds;
	}


    //poll(time):取走BlockingQueue里排在首位的对象,若不能立即取出,则可以等time参数规定的时间,取不
    // 到时返回null,此处为立即返回一个进程，如果不存在则返回失败
	public Runnable poll() {
        //当取得进程队列queue上的锁时（也就是当前对也queue对象没有其他操作，其他的函数没有正在访问queue对象），
        // 判断延时时间有没有到，如果没到延时时间或者当前队列为空的时候
        // 调用sleep 方法使得获取进程对象的操作延时100毫秒
		synchronized (queue) {
			while ((System.currentTimeMillis() - lastSuccesfullPop <= delayInMilliseconds) && !queue.isEmpty()) {
				sleep();
			}
            //记录成功获取进程对象的时间
			lastSuccesfullPop = System.currentTimeMillis();
            //调用queue接口的poll方法，获取队列中的一个进程对象
			return queue.poll();
		}
	}

	public Runnable poll(final long timeout, final TimeUnit unit) throws InterruptedException {
		synchronized (queue) {
			while ((System.currentTimeMillis() - lastSuccesfullPop <= delayInMilliseconds) && !queue.isEmpty()) {
				sleep();
			}
			lastSuccesfullPop = System.currentTimeMillis();
			return queue.poll(timeout, unit);
		}
	}

    //take()取走BlockingQueue里排在首位的对象,若BlockingQueue为空,阻断进入等待状态直到BlockingQueue
    // 有新的数据被加入;
    public Runnable take() throws InterruptedException {
		synchronized (queue) {
			while ((System.currentTimeMillis() - lastSuccesfullPop <= delayInMilliseconds) && !queue.isEmpty()) {
				sleep();
			}
			lastSuccesfullPop = System.currentTimeMillis();
			return queue.take();
		}
	}

    //同poll()方法的唯一不同在于当队列为空的时候抛出NoSuchElementException的异常
	public Runnable remove() {
		synchronized (queue) {
			while ((System.currentTimeMillis() - lastSuccesfullPop <= delayInMilliseconds) && !queue.isEmpty()) {
				sleep();
			}
			lastSuccesfullPop = System.currentTimeMillis();
			return queue.remove();
		}
	}

    //内部方法，当从队列中获取一个进程对象的时间距离上一次获取进程对象的时间小于延时时间时，调用sleep（）
    //方法延时100毫秒
	private void sleep() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Delegate Methods. Java is just soooo fun sometimes...

	public boolean add(final Runnable e) {
		return queue.add(e);
	}

	public boolean addAll(final Collection<? extends Runnable> c) {
		return queue.addAll(c);
	}

	public void clear() {
		queue.clear();
	}

	public boolean contains(final Object o) {
		return queue.contains(o);
	}

	public boolean containsAll(final Collection<?> c) {
		return queue.containsAll(c);
	}

	public int drainTo(final Collection<? super Runnable> c, final int maxElements) {
		return queue.drainTo(c, maxElements);
	}

	public int drainTo(final Collection<? super Runnable> c) {
		return queue.drainTo(c);
	}

	public Runnable element() {
		return queue.element();
	}

	@Override
	public boolean equals(final Object o) {
		return queue.equals(o);
	}

	@Override
	public int hashCode() {
		return queue.hashCode();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public Iterator<Runnable> iterator() {
		return queue.iterator();
	}

	public boolean offer(final Runnable e, final long timeout, final TimeUnit unit) throws InterruptedException {
		return queue.offer(e, timeout, unit);
	}

	public boolean offer(final Runnable e) {
		return queue.offer(e);
	}

	public Runnable peek() {
		return queue.peek();
	}

	public void put(final Runnable e) throws InterruptedException {
		queue.put(e);
	}

	public int remainingCapacity() {
		return queue.remainingCapacity();
	}

	public boolean remove(final Object o) {
		return queue.remove(o);
	}

	public boolean removeAll(final Collection<?> c) {
		return queue.removeAll(c);
	}

	public boolean retainAll(final Collection<?> c) {
		return queue.retainAll(c);
	}

	public int size() {
		return queue.size();
	}

	public Object[] toArray() {
		return queue.toArray();
	}

	public <T> T[] toArray(final T[] a) {
		return queue.toArray(a);
	}

}
