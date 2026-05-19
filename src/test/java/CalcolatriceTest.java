/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalcolatriceTest {

    @Test
    public void testSomma() {
        int risultato = 2 + 3;
        assertEquals(5, risultato, "La somma di 2 e 3 deve essere 5");
    }
}
