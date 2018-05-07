import java.lang.*;
import java.util.*;

import java.util.concurrent.*;

class Parallel{

  static <T> List<T> process(List<Callable<T>> lCallables){
    System.out.println("Going Parallel...");
    Stopwatch stopwatch = new Stopwatch();
    List<T> lResults = new ArrayList<T>();
    ExecutorService executor = null;
    try{
      executor = Executors.newFixedThreadPool(Config.config.iNumThreads);
      List<Future<T>> lFutures = executor.invokeAll(lCallables);
      executor.shutdownNow();
      for(Future<T> future : lFutures){
        Misc.Assert(future.isDone());
        lResults.add(future.get());
      }
    } catch(ExecutionException|InterruptedException ex){
      throw new RuntimeException(ex);
    } finally {
      if(executor != null){
        executor.shutdownNow();
        System.out.println("Terminating...");
        System.out.println("Parallel Time: " + stopwatch.secs());
        try{
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }catch(InterruptedException ex){
          throw new RuntimeException(ex);
        }
        Misc.Assert(executor.isTerminated());
        System.out.println("Terminated.");
      }
    }
    return lResults;
  }
}