# TEST

```mermaid
classDiagram

class Subject {
  <<interface>>
  +topic() String
  +id() String
}

class SharedCatalog~K extends Subject, T~ {
    +start() void
    
}

```



