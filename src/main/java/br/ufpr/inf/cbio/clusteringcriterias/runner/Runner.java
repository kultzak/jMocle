/*
 * Copyright (C) 2019 Gian Fritsche <gmfritsche at inf.ufpr.br>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpr.inf.cbio.clusteringcriterias.runner;

import br.ufpr.inf.cbio.clusteringcriterias.algorithm.builders.CLUIBEABuilder;
import br.ufpr.inf.cbio.clusteringcriterias.algorithm.builders.MOEAD.CLUMOEAD;
import br.ufpr.inf.cbio.clusteringcriterias.algorithm.builders.MOEAD.CLUMOEADBuilder;
import br.ufpr.inf.cbio.clusteringcriterias.algorithm.builders.NSGAIII.CLUNSGAIIIBuilder;
import br.ufpr.inf.cbio.clusteringcriterias.criterias.ObjectiveFunction;
import br.ufpr.inf.cbio.clusteringcriterias.criterias.impl.Connectivity;
import br.ufpr.inf.cbio.clusteringcriterias.criterias.impl.OverallDeviation;
import br.ufpr.inf.cbio.clusteringcriterias.dataset.Dataset;
import br.ufpr.inf.cbio.clusteringcriterias.dataset.DatasetFactory;
import br.ufpr.inf.cbio.clusteringcriterias.operator.*;
import br.ufpr.inf.cbio.clusteringcriterias.problem.ClusterProblem;
import br.ufpr.inf.cbio.clusteringcriterias.problem.Utils;
import br.ufpr.inf.cbio.clusteringcriterias.runner.experiment.ExperimentAlgorithmMOCLE;
import br.ufpr.inf.cbio.clusteringcriterias.solution.PartitionSolution;

import cern.colt.matrix.DoubleMatrix2D;
import jep.JepException;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.pesa2.PESA2Builder;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.mutation.NullMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Gian Fritsche <gmfritsche at inf.ufpr.br>
 */
public class Runner {

    public void run() throws FileNotFoundException, JepException {

        Dataset dataset = DatasetFactory.getInstance().getDataset(DatasetFactory.DATASET.ds2c2sc13_V3.toString());

        Problem problem;
        CrossoverOperator<PartitionSolution> crossover;
        MutationOperator<PartitionSolution> mutation;
        SelectionOperator<List<PartitionSolution>, PartitionSolution> selection;

        List<ObjectiveFunction> functions = new ArrayList<>();
        functions.add(new OverallDeviation(dataset));

        List<List<Integer>> neighborhood = Utils.computeNeighborhood(dataset.getDistanceMatrix());

        functions.add(new Connectivity(neighborhood));

        problem = new ClusterProblem(true, dataset, functions);

//        crossover = new HBGFCrossover(20);
        crossover = new HBGFCrossover();
//        crossover = new MixedHBGFCrossover();
//        crossover = new MPHBGFCrossover(20);
//        crossover = new CSPACrossover(dataset);
//        crossover = new MCLACrossover();
//        crossover = new CECrossover();

//        mutation = new ClusterRandomMutation(0.1);
//        mutation = new CentroidBasedMutation(1, dataset);
        mutation = new NullMutation<>();

        selection = new BinaryTournamentSelection<>(
                new RankingAndCrowdingDistanceComparator<>());

        int popSize = ((ClusterProblem) problem).getPopulationSize();
        int maxFitnessEvaluations = popSize * 1;
        System.out.println(popSize);

        //gera os vetores de peso para utilizar quando necessário
        Utils.generateWeightVector(popSize + (popSize % 2)); //todo: gerar os vetores de peso necessarios para os demais algoritmos


        Algorithm<List<PartitionSolution>> algorithm = new NSGAIIBuilder<>(problem,crossover,mutation,popSize + (popSize % 2))
                .setSelectionOperator(selection)
                .setOffspringPopulationSize((popSize + (popSize % 2))/2)
                .setMaxEvaluations(maxFitnessEvaluations)
                .build();

//            Algorithm<List<PartitionSolution>> algorithm = new CLUNSGAIIIBuilder<>(problem, crossover, mutation)
//                    .setSelection(selection)
//                    .setPopulationSize(popSize + (popSize % 2))
//                    .setMaxEvaluations(maxFitnessEvaluations)
//                    .build();




        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute();

        long computingTime = algorithmRunner.getComputingTime();
        JMetalLogger.logger.log(Level.INFO, "Total execution time: {0}ms", computingTime);

        List<PartitionSolution> population = SolutionListUtils.getNondominatedSolutions(algorithm.getResult());

        Utils.removeRepeated(population);

        System.out.println("Result population size: "+population.size());

        Utils.computeAdjustedRand(dataset.getTruePartition(),population, new DefaultFileOutputContext("ARI.tsv"));

        new SolutionListOutput(population)
                .setSeparator("\t")
                .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
                .print();

    }

    public static void main(String[] args) throws FileNotFoundException, JepException {
        (new Runner()).run();
    }

}
