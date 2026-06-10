# AI Modules - Architecture Diagrams

## 1. AI Validation Pipeline Flow

```mermaid
flowchart TD
    A[User Question + AI Response] --> B{FeatureFlagManager}
    B -->|Flags Checked| C[AIValidationManager]
    
    C --> D[Step 1: Keyword Validation]
    D -->|keywords found| E[Step 2: AI Judge Validation]
    D -->|no keywords| FAIL1[FAIL: Missing Keywords]
    
    E -->|PASS| F[Step 3: Semantic Evaluation]
    E -->|FAIL| FAIL2[FAIL: Judge Rejected]
    
    F --> G[SemanticEvaluationResult]
    G -->|"Combined Score = 60% Semantic + 40% Judge"| H{Score >= 0.70?}
    H -->|Yes| I[Step 4: Hallucination Check]
    H -->|No| FAIL3[FAIL: Low Semantic Score]
    
    I -->|No Hallucination| J[Step 5: Prompt Injection Check]
    I -->|Hallucination Detected| FAIL4[FAIL: Hallucination]
    
    J -->|Safe| K[Step 6: Context Validation]
    J -->|Injection Detected| FAIL5[FAIL: Injection Detected]
    
    K -->|Valid| PASS[✅ ALL VALIDATIONS PASSED]
    K -->|Invalid Context| FAIL6[FAIL: Context Mismatch]
    
    PASS --> L[AIValidationResult - Builder]
    FAIL1 --> L
    FAIL2 --> L
    FAIL3 --> L
    FAIL4 --> L
    FAIL5 --> L
    FAIL6 --> L
    
    L --> M[TokenUsageTracker Records]
    L --> N[AIReportingIntegration Records]
```

## 2. Locator Healing Flow

```mermaid
flowchart TD
    A[Element Not Found Exception] --> B{Healenium Available?}
    
    B -->|Yes| C[Healenium Self-Healing]
    B -->|No| D[LocatorHealingManager.healWithConfidence]
    
    C -->|Healed| R1[LocatorHealingResult<br/>source=HEALENIUM<br/>confidence=1.0]
    C -->|Failed| D
    
    D --> E{Check LocatorRepository Cache}
    E -->|Cache Hit| R2[LocatorHealingResult<br/>source=CACHE<br/>confidence=0.90]
    E -->|Cache Miss| F[GeminiLocatorGenerator]
    
    F --> G[Gemini API Call with Page Context]
    G --> H[Receive Suggested Locators]
    H --> I{Confidence >= 0.75?}
    
    I -->|Yes - Auto Heal| J[Store in LocatorRepository]
    J --> R3[LocatorHealingResult<br/>source=GEMINI<br/>confidence=score]
    
    I -->|No - Manual Review| R4[LocatorHealingResult<br/>source=GEMINI<br/>confidence=score<br/>requiresReview=true]
    
    style R1 fill:#28a745,color:#fff
    style R2 fill:#17a2b8,color:#fff
    style R3 fill:#28a745,color:#fff
    style R4 fill:#ffc107,color:#000
```

## 3. Embedding Provider Strategy Pattern

```mermaid
classDiagram
    class EmbeddingProvider {
        <<interface>>
        +generateEmbedding(String text) double[]
        +getEmbeddingDimension() int
        +getProviderName() String
        +isAvailable() boolean
    }
    
    class GeminiEmbeddingProvider {
        -geminiApiKey: String
        -MODEL: String = "text-embedding-004"
        -DIMENSION: int = 768
        +generateEmbedding(text) double[]
        +getEmbeddingDimension() int = 768
        +getProviderName() String = "Gemini"
        +isAvailable() boolean
    }
    
    class LocalEmbeddingProvider {
        -DIMENSION: int = 128
        +generateEmbedding(text) double[]
        +getEmbeddingDimension() int = 128
        +getProviderName() String = "Local"
        +isAvailable() boolean = true
    }
    
    class SemanticValidator {
        -embeddingProvider: EmbeddingProvider
        -threshold: double
        +validate(expected, actual) Map
        -getEmbeddingVector(text) double[]
    }
    
    EmbeddingProvider <|.. GeminiEmbeddingProvider
    EmbeddingProvider <|.. LocalEmbeddingProvider
    SemanticValidator --> EmbeddingProvider : uses
```

## 4. Token Usage Tracking Flow

```mermaid
sequenceDiagram
    participant Test as Test Step
    participant VM as AIValidationManager
    participant API as GeminiAPI
    participant TT as TokenUsageTracker
    participant Report as AIReportingIntegration
    
    Test->>VM: validateResponse(question, response, expected)
    VM->>API: callGemini(judgePrompt)
    API-->>VM: judge response
    VM->>TT: recordUsage("ai_judge", promptTokens, responseTokens)
    
    VM->>API: generateEmbedding(text)
    API-->>VM: embedding vector
    VM->>TT: recordUsage("embedding", promptTokens, 0)
    
    VM->>API: callGemini(hallucinationPrompt)
    API-->>VM: hallucination check
    VM->>TT: recordUsage("hallucination", promptTokens, responseTokens)
    
    VM-->>Test: AIValidationResult
    Test->>TT: getSummary()
    TT-->>Test: UsageSummary (tokens, cost, calls)
    Test->>Report: recordValidation(scenario, result)
    Report->>Report: generateSummaryHtml()
```

## 5. Feature Flag Decision Tree

```mermaid
flowchart LR
    A[config.properties] --> C{FeatureFlagManager}
    B[System Properties] --> C
    
    C --> D{ai.healing.enabled}
    C --> E{ai.judge.enabled}
    C --> F{semantic.validation.enabled}
    C --> G{hallucination.enabled}
    C --> H{prompt.injection.enabled}
    C --> I{context.validation.enabled}
    
    D -->|true| D1[LocatorHealingManager Active]
    D -->|false| D2[Healing Skipped]
    
    E -->|true| E1[AIJudgeValidator Runs]
    E -->|false| E2[Judge Skipped]
    
    F -->|true| F1[SemanticValidator Runs]
    F -->|false| F2[Semantic Skipped]
    
    G -->|true| G1[HallucinationValidator Runs]
    G -->|false| G2[Hallucination Skipped]
    
    H -->|true| H1[PromptInjectionValidator Runs]
    H -->|false| H2[Injection Skipped]
    
    I -->|true| I1[ContextValidator Runs]
    I -->|false| I2[Context Skipped]
```

## 6. TestDataManager Architecture

```mermaid
flowchart TD
    A[TestDataManager] --> B{ExecutionType}
    
    B --> C[POSITIVE<br/>positive.csv]
    B --> D[NEGATIVE<br/>negative.csv]
    B --> E[HALLUCINATION<br/>hallucination.csv]
    B --> F[INJECTION<br/>injection.csv]
    B --> G[CONTEXT<br/>context.csv]
    
    C --> H[EnumMap Cache]
    D --> H
    E --> H
    F --> H
    G --> H
    
    H --> I[getDataset - Full List]
    H --> J[getRandomSubset - Randomized N]
    H --> K[getByCategory - Filtered]
    H --> L[addEntry - Dynamic]
    
    subgraph "CSV Format"
        M[question,expectedAnswer,category]
    end
    
    subgraph "Classpath"
        N[ai-testdata/positive.csv]
        O[ai-testdata/negative.csv]
        P[ai-testdata/hallucination.csv]
        Q[ai-testdata/injection.csv]
        R[ai-testdata/context.csv]
    end
```

## 7. Complete System Architecture

```mermaid
graph TB
    subgraph "Test Layer (src/test)"
        T1[Cucumber Steps]
        T2[LocatorHealingAdapter]
        T3[TestNGExtentReportListener]
    end
    
    subgraph "AI Core (src/main)"
        subgraph "Validators"
            V1[AIValidationManager]
            V2[AIJudgeValidator]
            V3[SemanticValidator]
            V4[HallucinationValidator]
            V5[PromptInjectionValidator]
            V6[ContextValidator]
        end
        
        subgraph "Embedding"
            E1[EmbeddingProvider Interface]
            E2[GeminiEmbeddingProvider]
            E3[LocalEmbeddingProvider]
        end
        
        subgraph "Locator Healing"
            L1[LocatorHealingManager]
            L2[GeminiLocatorGenerator]
            L3[LocatorRepository]
            L4[LocatorHealingResult]
        end
        
        subgraph "Infrastructure"
            I1[FeatureFlagManager]
            I2[TokenUsageTracker]
            I3[AIReportingIntegration]
            I4[TestDataManager]
        end
        
        subgraph "Utils"
            U1[GeminiAPI]
            U2[EmbeddingUtil]
            U3[SimilarityUtil]
        end
    end
    
    T1 --> V1
    T2 --> L1
    T3 --> I3
    
    V1 --> I1
    V1 --> V2
    V1 --> V3
    V1 --> V4
    V1 --> V5
    V1 --> V6
    V1 --> I2
    
    V3 --> E1
    E1 --> E2
    E1 --> E3
    
    L1 --> L2
    L1 --> L3
    L1 --> L4
    
    V2 --> U1
    V4 --> U1
    V5 --> U1
    L2 --> U1
    E2 --> U1
    V3 --> U3
```
