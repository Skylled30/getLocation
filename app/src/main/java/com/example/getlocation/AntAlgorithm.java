package com.example.getlocation;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class AntAlgorithm {

    //инициализируем
    public static final double alpha = 1;
    public static final double betta = 1;
    public static final int q = 20; //для нахождения deltaTau
    private static final double P = 0.3; //для обновления феромона

    public static Random random = new Random(); //рандом

    public static final int iterations = 1000; //количество проходов
    public static double Lmin = 10000; //минимальная длина

    public static final int b = 1;

    public static int number;

    public static String leadTime = "";
    static ArrayList<Point> nodes;

    public static double[][] cities;
    public static double[][] pheromone;

    //1 8 6 10 2 4 7 5 9 3
//    public static double[][] cities = {{9, 9, 9, 9, 9, 9, 9, b, 9, 9},
//            {9, 9, 9, b, 9, 9, 9, 9, 9, 9},
//            {b, 9, 9, 9, 9, 9, 9, 9, 9, 9},
//            {9, 9, 9, 9, 9, 9, b, 9, 9, 9},
//            {9, 9, 9, 9, 9, 9, 9, 9, b, 9},
//            {9, 9, 9, 9, 9, 9, 9, 9, 9, b},
//            {9, 9, 9, 9, b, 9, 9, 9, 9, 9},
//            {9, 9, 9, 9, 9, b, 9, 9, 9, 9},
//            {9, 9, b, 9, 9, 9, 9, 9, 9, 9},
//            {9, b, 9, 9, 9, 9, 9, 9, 9, 9}};

    public static int[] Tmin; //минимальный маршрут

    public AntAlgorithm(int _number, ArrayList<Point> points) {
        number = _number;
        nodes = points;
        cities = new double[number][number];
        pheromone = new double[number][number];
        Tmin = new int[cities.length];
    }

    public static void start() {
        Log.d("mytag", "here2 ");
//        //создание матрицы расстояний
        for(int i = 0; i < number; i++){
            for(int j = 0; j < number; j++){
                double x = Math.abs((nodes.get(j).getX() - nodes.get(i).getX()) * (nodes.get(j).getX() - nodes.get(i).getX()));
                double y = Math.abs((nodes.get(j).getY() - nodes.get(i).getY()) * (nodes.get(j).getY() - nodes.get(i).getY()));
                String xx = String.valueOf(x);
                String yy = String.valueOf(x);
                if(xx.length() > 8)
                    x = Double.parseDouble(String.valueOf(x).substring(0, 8));
                if(yy.length() > 8)
                    y = Double.parseDouble(String.valueOf(y).substring(0, 8));
                cities[i][j] = Math.sqrt(x + y);
            }
        }

        for (int i = 0; i < number; i++) {
            Log.d("mytag", Arrays.toString(cities[i]));
        }
        long startTime = System.currentTimeMillis(); // записываем начальное время
        //создаём муравьёв
        Ant[] ants = new Ant[50]; //количество муравьёв
        for (int i = 0; i < ants.length; i++) {
            ants[i] = new Ant();
            ants[i].setCurrCity(1);
        }
        //создаём рандомную матрицу путей и феромона
        for (int i = 0; i < cities.length; i++) {
            for (int j = 0; j < cities.length; j++) {
                if (i != j) {
//                    cities[i][j] = random.nextInt(50) + 1;
                    pheromone[i][j] = random.nextInt(5) + 1;
                } else
                    cities[i][j] = pheromone[i][j] = 0;
            }
        }
        Log.d("mytag", "here3 ");

        System.out.println("Начать поиск кратчайшего пути? \nДа - 1, нет - 0");
        if(1 == 1) {

            //основной цикл
            for (int u = 0; u < iterations; u++) {

                //идём по всем муравьям, ищем маршрут, и записываем его
                //длину и пройденные города
                for (int t = 0; t < ants.length; t++) {
                    ants[t] = calculationOfRoutes(ants[t]);
                    ants[t].setCurrCity(1);
                }

                //Проверка всех маршрутов на самый кратчайший
                for (int i = 0; i < ants.length; i++) {
                    if(ants[i].getDistance() < Lmin){
                        Lmin = ants[i].getDistance();
                        Tmin = ants[i].getRoute();
                    }
                }

                //обновляем следы феромона
                for (Ant ant : ants) {
                    double deltaTau;
                    for (int t = 1; t < ant.getRoute().length; t++) {
                        int i = ant.getRoute()[t - 1] - 1;
                        int j = ant.getRoute()[t] - 1;
                        deltaTau = q / cities[i][j];
                        pheromone[i][j] = (1 - P) * pheromone[i][j] + deltaTau;
                    }
                }
            }
        }
//        System.out.println("Кратчайшая длина - " + Lmin + ", accuracy " + 600 / Lmin * 100);
//        System.out.println("Кратчайший маршрут - " + Arrays.toString(Tmin));
        long timeSpent = System.currentTimeMillis() - startTime;
//        System.out.println("Программа выполнялась " + (timeSpent / 1000.0) + " секунд");
        leadTime = (timeSpent / 1000.0) + " секунд";
    }

    //считает пройденный путь и запоминает маршрут
    public static Ant calculationOfRoutes(Ant ant)
    {
        double roulette = 0;
        double pSum = 0;

        //список города, куда можно пойти
        ArrayList<Integer> city = new ArrayList<>();
        for (int j = 0; j < cities.length; j++) {
            //чтобы не прибавлять самого себя
            if(j + 1 != ant.getCurrCity()) {
                city.add(j + 1);
            }
        }
        //массив передвижений
        int[] route = new int[cities.length];
        route[0] = ant.getCurrCity();

        //пройденный путь
        double way = 0;

        //знаменатель вероятности
        double pDen = 0;

        for (int i = 1; i <= cities.length; i++) {
            //обнуляем сумму для рулетки
            pSum = 0;

            //создаём рандомное число
            roulette = random.nextDouble();

            //знаменатель формулы считаем заранее, чтобы каждый раз не считать в цикле
            pDen = availableCities(city, ant.getCurrCity());

            for (int j = 1; j <= cities.length; j++) {
                if(city.contains(j) && ant.getCurrCity() != j){
                    pSum += ((1 / cities[ant.getCurrCity() - 1][j - 1]) * pheromone[ant.getCurrCity() - 1][j - 1]) / pDen;
                }
                if(pSum > roulette) {
                    way += cities[ant.getCurrCity() - 1][j - 1];
                    ant.setCurrCity(j);
                    city.remove(new Integer(ant.getCurrCity()));
                    route[i] = j;
                    break;
                }
            }
        }
        ant.setRoute(route);
        ant.setDistance(way);
        return ant;
    }

    private static double availableCities(ArrayList<Integer> city, int currCity) {
        double sum = 0;
        for (int i = 1; i <= cities.length; i++) {
            if(city.contains(i) && currCity != i){
                sum += (1/cities[currCity-1][i-1]) * pheromone[currCity-1][i-1];
            }
        }
        return sum;
    }

    public static class Ant {
        private int currCity;
        private double distance;
        private int[] route;

        @Override
        public String toString() {
            return Arrays.toString(route);
        }

        public int getCurrCity() {
            return currCity;
        }

        public void setCurrCity(int currCity) {
            this.currCity = currCity;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public int[] getRoute() {
            return route;
        }

        public void setRoute(int[] route) {
            this.route = route;
        }
    }
}
