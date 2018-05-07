import org.apache.commons.lang3.time.StopWatch;

class Stopwatch{
  org.apache.commons.lang3.time.StopWatch stopwatch;
  Stopwatch(){
    stopwatch = new org.apache.commons.lang3.time.StopWatch();
    stopwatch.start();
  }

  double secs(){
    return (((double)stopwatch.getTime())/1000.0);
  }
}