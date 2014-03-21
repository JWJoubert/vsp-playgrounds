/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.taxi.optimizer.mip;

import java.io.*;
import java.util.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;


public class MIPProblem
{
    static class MIPSolution
    {
        final boolean[][] x;
        final double[] w;


        MIPSolution(boolean[][] x, double[] w)
        {
            this.x = x;
            this.w = w;
        }
    };


    enum Mode
    {
        OFFLINE_INIT_OPTIM(true, true, false, 99999), //
        OFFLINE_INIT(true, false, false, 99999), //
        OFFLINE_OPTIM(false, true, false, 99999), //
        OFFLINE_LOAD(false, false, true, 99999), //
        //
        ONLINE_1(true, true, false, 1), //
        ONLINE_2(true, true, false, 2), //
        ONLINE_3(true, true, false, 3), //
        ONLINE_4(true, true, false, 4), //
        ONLINE_5(true, true, false, 5);

        private final boolean init;
        private final boolean optim;
        private final boolean load;
        private final int reqsPerVeh;//planning horizon


        private Mode(boolean init, boolean optim, boolean load, int reqsPerVeh)
        {
            this.init = init;
            this.optim = optim;
            this.load = load;
            this.reqsPerVeh = reqsPerVeh;
        }
    };


    private final TaxiOptimizerConfiguration optimConfig;
    private final PathTreeBasedTravelTimeCalculator pathTravelTimeCalc;

    private SortedSet<TaxiRequest> unplannedRequests;
    private MIPRequestData rData;
    private VehicleData vData;

    private MIPSolution initialSolution;
    private MIPSolution finalSolution;

    static final Mode mode = Mode.ONLINE_5;


    public MIPProblem(TaxiOptimizerConfiguration optimConfig,
            PathTreeBasedTravelTimeCalculator pathTravelTimeCalc)
    {
        this.optimConfig = optimConfig;
        this.pathTravelTimeCalc = pathTravelTimeCalc;
    }


    public void scheduleUnplannedRequests(SortedSet<TaxiRequest> unplannedRequests)
    {
        this.unplannedRequests = unplannedRequests;

        initData();
        if (vData.dimension == 0 || rData.dimension == 0) {
            return;
        }

        if (mode.init) {
            findInitialSolution();
        }

        if (mode.optim) {
            solveProblem();
        }
        else if (mode.load) {
            loadSolution(optimConfig.workingDirectory + "gurobi_solution.sol");
        }
        else if (mode.init) {
            finalSolution = initialSolution;
        }
        else {
            throw new RuntimeException();
        }

        scheduleSolution();
    }


    private void initData()
    {
        List<TaxiRequest> removedRequests = optimConfig.scheduler
                .removePlannedRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);

        vData = new VehicleData(optimConfig);
        if (vData.dimension == 0) {
            return;
        }

        int maxReqCount = mode.reqsPerVeh * vData.dimension;
        rData = new MIPRequestData(optimConfig, unplannedRequests, maxReqCount);
        if (rData.dimension == 0) {
            return;
        }
    }


    private void findInitialSolution()
    {
        initialSolution = new MIPSolutionFinder(optimConfig, rData, vData).findInitialSolution();
        optimConfig.scheduler.removePlannedRequestsFromAllSchedules();
    }


    private void solveProblem()
    {
        finalSolution = new MIPGurobiSolver(optimConfig, pathTravelTimeCalc, rData, vData)
                .solve(initialSolution);
    }


    private void scheduleSolution()
    {
        new MIPSolutionScheduler(optimConfig, rData, vData).updateSchedules(finalSolution);
        unplannedRequests.removeAll(Arrays.asList(rData.requests));
    }


    private void loadSolution(String filename)
    {
        Scanner s;
        try {
            s = new Scanner(new File(filename));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //header
        if (!s.nextLine().startsWith("# Objective value = ")) {
            s.close();
            throw new RuntimeException();
        }

        int n = rData.dimension;
        int m = vData.dimension;

        boolean[][] x = new boolean[m + n][m + n];
        for (int u = 0; u < m + n; u++) {
            for (int v = 0; v < m + n; v++) {

                //line format: x_430,430 0
                if (!s.next().equals("x_" + u + "," + v)) {
                    s.close();
                    throw new RuntimeException();
                }

                x[u][v] = s.nextDouble() >= 0.5;
            }
        }

        double[] w = new double[n];
        for (int i = 0; i < n; i++) {

            //line format: w_0 22096
            if (!s.next().equals("w_" + i)) {
                s.close();
                throw new RuntimeException();
            }

            w[i] = s.nextDouble();
        }

        s.close();
        finalSolution = new MIPSolution(x, w);
    }
    
    
    int getPlanningHorizon()
    {
        return vData.dimension * mode.reqsPerVeh;
    }
    
    
    int getPlannedRequestCount()
    {
        return rData.dimension;
    }
}
