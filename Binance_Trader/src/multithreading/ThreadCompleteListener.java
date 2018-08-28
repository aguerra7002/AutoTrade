package multithreading;


/** 
 * Very simple quick interface that we will use to make us able to notify classes when the threads they 
 * spawn complete.
 * @author alexg
 *
 */
public interface ThreadCompleteListener {
    void notifyOfThreadComplete(final Thread thread);
}
