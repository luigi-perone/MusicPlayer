## Metodologia di Stima dell'Effort

Questo documento definisce l'approccio metodologico adottato dal team per la stima dell'effort associato alle *User Story*. Al fine di garantire stime rapide, collaborative e semplici da effettuare, il gruppo utilizza due strumenti: il **Metodo Fibonacci** e il **Planning Poker**.

---

### 1. Il Metodo Fibonacci (Unità di Misura)

La misurazione del lavoro non avviene in ore o giorni lavorativi, metriche soggette a forte variabilità individuale, ma in **Story Points**. Viene utilizzata una scala basata sulla sequenza di Fibonacci modificata (1, 2, 3, 5, 8, 13, 21...) (nel nostro caso consideriamo i numeri fino a 13).

Ogni punteggio riflette l'insieme di tre fattori chiave:
* **Complessità**: il livello di difficoltà dell'implementazione.
* **Quantità di lavoro**: il volume di codice o di attività richieste.
* **Incertezza e Rischio**: il grado di chiarezza dei requisiti e la presenza di dipendenze esterne o tecnologie non padroneggiate.

### 2. Il Planning Poker (Metodo di Stima)

Il *Planning Poker* è la cerimonia di stima collettiva in cui il team si riunisce per valutare il Product Backlog. Il processo segue questi passaggi:
1. Il Product Owner presenta una specifica *User Story*.
2. Il team discute i dettagli tecnici, i confini e i criteri di accettazione (*Definition of Done*).
3. Ciascun membro assegna segretamente un punteggio tramite una carta della sequenza di Fibonacci.
4. Le carte vengono scoperte simultaneamente:
   * **Consenso**: se i voti sono allineati, il punteggio viene confermato.
   * **Discrepanza**: in caso di valori distanti, le parti si confrontano per chiarire i dubbi e si procede a una nuova votazione.

### 3. Vantaggi (Fase di Pre-Game)

L'uso combinato di questi metodi offre vantaggi pratici e organizzativi per la gestione degli Sprint:

* **Stime più realistiche** (no "falsa precisione"): usare la sequenza di Fibonacci (es. saltare da 5 a 8, poi a 13) evita di focalizzarsi sul conteggio delle singole ore di lavoro. Ci si concentra invece sulla reale complessità del problema, accettando l'incertezza tipica dello sviluppo software.
* **Confronto tecnico preventivo**: se durante il Planning Poker emergono voti molto diversi, il team è spinto a confrontarsi immediatamente. Questo permette di chiarire dubbi sui requisiti o sull'architettura prima ancora di iniziare a scrivere il codice.
* **Voto indipendente** (nessun effetto ancoraggio): votare tutti insieme e in segreto garantisce che le stime siano oggettive. In questo modo si evita che l'opinione di uno o più membri esperti, o di chi parla per primo, influenzi il giudizio degli altri.
* **Creazione di una Baseline Condivisa**: la stima del set iniziale calibra il "metro" del team. L'identificazione di una *User Story* di riferimento (es. valore 3) fungerà da punto di riferimento per le stime successive.

---

### 4. Scala dei Punteggi per il Team

Per garantire stime coerenti durante il Planning Poker, il team utilizza la seguente griglia interpretativa. I valori della scala indicano il numero di componenti di sistema che devono interagire per completare una funzionalità; a punteggi più elevati si rischia un forte accoppiamento (High Coupling) dei sottosistemi e, di conseguenza, l'esigenza di un loro coordinamento complesso.

* **1 Story Point (Banale)**: Funzionalità che agisce su una singola entità, senza bisogno di comunicazione con il resto del sistema. Operazioni CRUD(crea, leggi, aggiorna ed elimina) di base realizzate con i componenti standard di JavaFX. Eventuali effetti automatici su altre entità sono diretti e non richiedono logica aggiuntiva.
* **2 - 3 Story Points (Semplice)**: Funzionalità che coinvolge due entità in modo lineare. Lo scambio è ben definito e avviene in un solo verso o in un flusso interattivo chiaro.
* **5 Story Points (Medio)**: Funzionalità che coinvolge più entità o viste, ognuna con un proprio ruolo. La comunicazione tra le parti va progettata da zero, ma resta lineare e priva di incognite rilevanti.
* **8 Story Points (Complesso)**: Funzionalità che fa interagire parti del sistema già esistenti in scenari nuovi. Il lavoro non sta nel definire pezzi inediti, ma nel mantenerli coerenti tra loro mentre comunicano in tempo reale, evitando possibili problemi di sincronizzazione.
* **13 Story Points (Molto Complesso)**: Funzionalità in cui il coordinamento tra le parti è così denso da richiedere una struttura dedicata che lo regga (uno stack, una coda composita, un pattern architetturale). Impegnativa, ma completabile in un singolo Sprint.