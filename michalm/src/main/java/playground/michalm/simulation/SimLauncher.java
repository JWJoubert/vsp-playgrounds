package playground.michalm.simulation;

import java.util.*;

import org.matsim.core.controler.*;


public class SimLauncher
{
    public static void main(String[] args)
    {
        String dirName;
        String cfgFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            cfgFileName = "config-verB.xml";
        }
        else if (args.length == 2) {
            dirName = args[0];
            cfgFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Controler controler = new Controler(new String[] { dirName
                + cfgFileName });
        controler.setOverwriteFiles(true);
        controler.run();
    }
}
