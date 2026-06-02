## Coding Convention

### Stile generale

* **Lingua codice, commit e commenti**: inglese
* Lingua documenti di progetto: italiano
* Usa nomi **espliciti** e **consistenti**

### Java Coding Convention

* **Naming convention**:

  * Classi: `PascalCase`
  * Metodi e variabili: `camelCase`
  * Costanti: `MAIUSCOLO_CON_UNDERSCORE`

* **Formattazione del codice**:

  * Spaziatura uniforme(4 spazi)
  * Indentazione corretta (usare macro IDE per il refactoring)
  * Uso delle parentesi graffe `{}` anche per blocchi monoriga

* **Definizione delle costanti**:

  * Evitare magic numbers, usare costanti simboliche in modo da
    rendere più semplice la modifica e la comprensione del codice

* **Commenti e documentazione**:

  * JavaDoc per tutte le classi e i metodi
  * Evitare commenti ovvi o ridondanti

* **Testing**:

  * Test con nomi descrittivi secondo lo standard JUnit 5
  * Testare il codice prima di fare push

* **Package naming**:

  * Solo lowercase, senza underscore, secondo le convenzioni Java. Esempio: `com.miodominio.nomeprogetto.modulo`

* **Commit convention**:

  * Scelta di titoli chiari per i commit, che descrivano il tipo di modifica o aggiunta apporta


