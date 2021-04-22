package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class CreateUserService {

    private final Connection connection;

    CreateUserService() throws SQLException {
        var url = "jdbc:sqlite:users_database.db";
        this.connection = DriverManager.getConnection(url);
        try {
            this.connection.createStatement().execute(createUserQuery());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws SQLException {
        var createUserService = new CreateUserService();
        try (var service = new KafkaService<>(CreateUserService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                createUserService::parse,
                Order.class,
                Map.of())) {
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Order> record) throws SQLException {
        System.out.println("------------------------------------------");
        System.out.println("Processing user, checking if is new");
        System.out.println(record.key());
        System.out.println(record.value());
        System.out.println(record.partition());
        System.out.println(record.offset());

        var order = record.value();

        if(isNewUser(order.getEmail())) {
            insertNewUser(order.getEmail());
        }
    }

    private void insertNewUser(String email) throws SQLException {

        StringBuilder query = new StringBuilder();

        query.append("  INSERT INTO                 ");
        query.append("      USERS   (uuid, email)   ");
        query.append("  VALUES                      ");
        query.append("      (?, ?)                  ");

        var insert = this.connection.prepareStatement(query.toString());

        var uuid = UUID.randomUUID().toString();

        insert.setString(1, uuid);
        insert.setString(2, email);

        System.out.printf("\nUsuário inserido com sucesso! email: %s, uuid: %s \n", email, uuid);
    }

    private boolean isNewUser(String email) throws SQLException {

        StringBuilder query = new StringBuilder();

        query.append("  SELECT                  ");
        query.append("      u.uuid              ");
        query.append("  FROM                    ");
        query.append("      USERS u             ");
        query.append("  WHERE                   ");
        query.append("      u.email = ? limit 1 ");

        var existsUser = connection.prepareStatement(query.toString());

        existsUser.setString(1, email);

        var uuidConsulted = existsUser.executeQuery();

        return !uuidConsulted.next();
    }

    public static final String createUserQuery() {

        StringBuilder query = new StringBuilder();

        query.append("  CREATE TABLE USERS (                ");
        query.append("      uuid varchar(255) primary key,  ");
        query.append("      email varchar(200))             ");

        return query.toString();
    }

}
