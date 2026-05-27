## Metodologia di Stima dell'Effort

Questo documento definisce l'approccio metodologico adottato dal team per la stima dell'effort associato alle *User Story*. Al fine di garantire stime rapide, collaborative e realistiche, il gruppo utilizza l'azione congiunta di due strumenti: il **Metodo Fibonacci** e il **Planning Poker**.

---

### 1. Il Metodo Fibonacci (Unità di Misura)

La misurazione del lavoro non avviene in ore o giorni lavorativi, metriche soggette a forte variabilità individuale, ma in **Story Points**. Viene utilizzata una scala basata sulla sequenza di Fibonacci modificata (1, 2, 3, 5, 8, 13, 21...).

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
* **Confronto tecnico preventivo**: se durante il Planning Poker emergono voti molto diversi, il team è spinto a confrontarsi immediatamente. Questo permette di chiarire dubbi sui requisiti o sull'architettura prima ancora di iniziare a scrivere codice.
* **Voto indipendente** (nessun effetto ancoraggio): votare tutti insieme e in segreto garantisce che le stime siano oggettive. In questo modo si evita che l'opinione del membro più esperto, o di chi parla per primo, influenzi il giudizio degli altri.
* **Creazione di una Baseline Condivisa**: la stima del set iniziale calibra il "metro" del team. L'identificazione di una *User Story* di riferimento (es. valore 3) fungerà da ancoraggio obiettivo per accelerare tutte le stime future.

---

### 4. Scala dei Punteggi per il Team

Per mantenere omogeneità e coerenza durante le sessioni di Planning Poker, il team fa riferimento alla seguente griglia interpretativa (La scala riflette quante parti del sistema devono comunicare tra loro per realizzare la funzionalità, e quanto stretto deve essere il loro coordinamento):

* **1 Story Point (Banale)**: Funzionalità che agisce su una singola entità, senza bisogno di comunicazione con il resto del sistema. Operazioni CRUD di base realizzate con i componenti standard di JavaFX. Eventuali effetti automatici su altre entità sono diretti e non richiedono logica aggiuntiva.
* **2 - 3 Story Points (Semplice)**: Funzionalità che coinvolge due entità in modo lineare. Lo scambio è ben definito e avviene in un solo verso o in un flusso interattivo chiaro.
* **5 Story Points (Medio)**: Funzionalità che coinvolge più entità o viste, ognuna con un proprio ruolo. La comunicazione tra le parti va progettata da zero, ma resta lineare e priva di forti incognite. Il lavoro consiste nel costruire qualcosa di nuovo con un perimetro chiaro.
* **8 Story Points (Complesso)**: Funzionalità che fa interagire parti del sistema già esistenti in scenari nuovi. Il lavoro non sta nel costruire pezzi inediti, ma nel mantenerli coerenti tra loro mentre comunicano in tempo reale, gestendo le conseguenze delle loro interazioni.
* **13 Story Points (Molto Complesso)**: Funzionalità in cui il coordinamento tra le parti è così denso da richiedere una struttura dedicata che lo regga (uno stack, una coda composita, un pattern architetturale). Impegnativa, ma completabile in un singolo Sprint.