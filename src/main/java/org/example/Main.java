package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client("localhost", 8081, 12);
        client.run();
    }
}

/*
 var response01 = MonkHttp.get("localhost", 8081, "list.txt");
            response01.forEach(System.out::println);

            var response02 = MonkHttp.head("localhost", 8081, "data/contact.txt");
            response02.forEach(System.out::println);

            var response03 = MonkHttp.range("localhost", 8081, "list.txt", 0, 2);
            response03.forEach(System.out::println);
* */