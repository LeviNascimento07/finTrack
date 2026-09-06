module com.fintrack {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Permite que o JavaFX/FXML acesse os controllers via reflexão
    opens com.fintrack.controller to javafx.fxml;
    opens com.fintrack.app to javafx.fxml;

    // Necessário para o PropertyValueFactory ler os getters do model
    opens com.fintrack.model to javafx.fxml;

    // Exporta o pacote principal (ponto de entrada)
    exports com.fintrack.app;
}