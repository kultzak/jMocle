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

import br.ufpr.inf.cbio.clusteringcriterias.criterias.ObjectiveFunction;
import br.ufpr.inf.cbio.clusteringcriterias.criterias.impl.Connectivity;
import br.ufpr.inf.cbio.clusteringcriterias.criterias.impl.OverallDeviation;
import br.ufpr.inf.cbio.clusteringcriterias.operator.HBGFCrossover;
import br.ufpr.inf.cbio.clusteringcriterias.problem.ClusterProblem;
import br.ufpr.inf.cbio.clusteringcriterias.dataset.Dataset;
import br.ufpr.inf.cbio.clusteringcriterias.dataset.DatasetFactory;
import br.ufpr.inf.cbio.clusteringcriterias.problem.Utils;
import br.ufpr.inf.cbio.clusteringcriterias.solution.PartitionSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
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
import org.uma.jmetal.util.point.util.distance.EuclideanDistance;

/**
 *
 * @author Gian Fritsche <gmfritsche at inf.ufpr.br>
 */
public class Runner {

    public void run() {

        Dataset dataset = DatasetFactory.getInstance().getDataset(DatasetFactory.DATASET.iris.toString());

        double crossoverProbability;
        Problem problem;
        CrossoverOperator<PartitionSolution> crossover;
        MutationOperator<PartitionSolution> mutation;
        SelectionOperator<List<PartitionSolution>, PartitionSolution> selection;

        List<ObjectiveFunction> functions = new ArrayList<>();
        functions.add(new OverallDeviation(dataset, new EuclideanDistance()));

        double[][] distanceMatrix = Utils.computeDistanceMatrix(dataset, new EuclideanDistance());
        for (double[] distances : distanceMatrix) {
            System.out.println(Arrays.toString(distances));
        }

        List<List<Integer>> neighborhood = Utils.computeNeighborhood(distanceMatrix);
        for (List<Integer> integers : neighborhood) {
            System.out.println(integers);
        }

        functions.add(new Connectivity(neighborhood));

        problem = new ClusterProblem(true, dataset, functions);

        crossoverProbability = 1.0;
        int numberOfGeneratedChild = 2;
        crossover = new HBGFCrossover(crossoverProbability, numberOfGeneratedChild);

        mutation = new NullMutation<>();

        selection = new BinaryTournamentSelection<>(
                new RankingAndCrowdingDistanceComparator<PartitionSolution>());

        int popSize = ((ClusterProblem) problem).getPopulationSize();
        int maxFitnessEvaluations = popSize * 50;
        System.out.println(popSize);

        Algorithm<List<PartitionSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation)
                .setSelectionOperator(selection)
                .setMaxEvaluations(maxFitnessEvaluations)
                .setPopulationSize(popSize + (popSize % 2))
                .build();

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute();

        long computingTime = algorithmRunner.getComputingTime();
        JMetalLogger.logger.log(Level.INFO, "Total execution time: {0}ms", computingTime);

        List<PartitionSolution> population = SolutionListUtils.getNondominatedSolutions(algorithm.getResult());
        Set<PartitionSolution> set = new LinkedHashSet<>();
        set.addAll(population);
        population.clear();
        population.addAll(set);

        for (PartitionSolution s : population) {
            for (int i = 0; i < s.getNumberOfVariables(); i++) {
                System.out.print(s.getVariableValue(i) + " ");
            }
            System.out.println(Arrays.toString(s.getObjectives()));
//            System.out.println(s.hashCode());
        }
        System.out.println(population.size());

    }

    public static void main(String[] args) {
        (new Runner()).run();
    }

}
