/* Copyright (c) 2010 - 2012, The University of Edinburgh.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import uk.ac.ed.ph.jacomax.JacomaxSimpleConfigurator;
import uk.ac.ed.ph.jacomax.MaximaConfiguration;
import uk.ac.ed.ph.jacomax.MaximaInteractiveProcess;
import uk.ac.ed.ph.jacomax.MaximaProcessLauncher;
import uk.ac.ed.ph.jacomax.utilities.MaximaOutputUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example that does a trivial Maxima call with exceptionally verbose logging.
 * <p>
 * (Might be useful for diagnosing and submitting bug reports.)
 *
 * @author David McKain
 */
public class JacomaxDiagnostic {

    private static final Logger logger = LoggerFactory.getLogger(JacomaxDiagnostic.class);

    public static void main(final String[] args) throws Exception {
      //setupLog4J();
        try {
            final MaximaConfiguration configuration = JacomaxSimpleConfigurator.configure();
            final MaximaProcessLauncher launcher = new MaximaProcessLauncher(configuration);
            final MaximaInteractiveProcess process = launcher.launchInteractiveProcess();

            final String result1 = process.executeCall("display2d:false;", 10);
            final String parsed1 = MaximaOutputUtilities.parseSingleLinearOutputResult(result1);
            System.out.println("parsed1:" + parsed1 + " Equal:" 
                               + parsed1.equals("false"));
            
            final String result = 
              process.executeCall("solve([x+y=d,a*x+b*y=c],[x,y]);", 10);
            final String parsed = MaximaOutputUtilities.parseSingleLinearOutputResult(result);
            process.terminate();
            System.out.println("Result is:\n" + result);
            System.out.println("Parsed Result is:\n" + parsed);
            // if ("1".equals(parsed)) {
            //     logger.info("JacomaxDiagnostic ran successfully");
            // }
            // else {
            //     logger.error("JacomaxDiagnostic did not work as expected!");
            //     logger.info("Raw result was: " + result);
            // }
        }
        catch (final Exception e) {
            logger.error("JacomaxDiagnostic throw an Exception when running", e);
        }
    }

    // private static void setupLog4J() throws Exception {
    //     /* We configure Log4J to be exceptionally verbose here, but without requiring
    //      * any config files to get loaded. We would normally do this directly-
    //      *-
    //      * BasicConfigurator.configure();
    //      * org.apache.log4j.Logger.getRootLogger().setLevel(Level.TRACE);
    //      *-
    //      * but I want to avoid a compile-time dependency, hence the following mucky
    //      * equivalent using reflection:
    //      */
    //     Class.forName("org.apache.log4j.BasicConfigurator").getMethod("configure").invoke(null);
    //     final Object rootLogger = Class.forName("org.apache.log4j.Logger").getMethod("getRootLogger").invoke(null);
    //     final Class<?> levelClass = Class.forName("org.apache.log4j.Level");
    //     final Object traceLevel = levelClass.getDeclaredField("TRACE").get(levelClass);
    //     rootLogger.getClass().getMethod("setLevel", levelClass).invoke(rootLogger, traceLevel);
    // }
}
