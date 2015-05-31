# JWKTL Use Cases #

This page focuses on use cases for accessing a certain kind of information type from Wiktionary, such as translations. Before taking a closer look at this page, make sure that JWKTL is correctly installed as explained in the [getting started](GettingStarted.md) guide and familiarize yourself with the basic [architecture of JWKTL](JWKTLArchitecture.md).

For brevity, we won't be able to describe the information types in detail, which is why we refer you to the publications mentioned on the project homepage and the Javadoc code documentation of the individual methods.

## Iterating over pages, entries, senses, relations ##

In addition to querying a specific dictionary article by its lemma (which we describe below), JWKTL facilitates iterating over all encoded entries. The corresponding methods make use of Java's iterable interface and can hence be easily used in a for loop. Note that iterating over all entries is computationally far more expensive than querying for a few individual entries, since querying is based on efficient database indexes. Below is some example code for iterating over all pages, entries, and senses and counting them.

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  int pageCount = 0;
  int entryCount = 0;
  int senseCount = 0;
  for (IWiktionaryPage page : wkt.getAllPages()) {
    for (IWiktionaryEntry entry : page.getEntries()) {
      senseCount += entry.getSenseCount();
      entryCount++;
    }
    pageCount++;
  }
  System.out.println("Pages: " + pageCount);
  System.out.println("Entries: " + entryCount);
  System.out.println("Senses: " + senseCount);
  wkt.close();
```

JWKTL also supports using filters that facilitate skipping unwanted entries during iteration. Imagine you want to compile a list of all German adjectives encoded in a Wiktionary language edition. The source code for extracting this list and printing its size could, for example, look like this:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  WiktionaryEntryFilter filter = new WiktionaryEntryFilter();
  filter.setAllowedWordLanguages(Language.GERMAN);
  filter.setAllowedPartsOfSpeech(PartOfSpeech.ADJECTIVE);
  int deAdjectiveCount = 0;
  for (IWiktionaryEntry entry : wkt.getAllEntries(filter)) {
    System.out.println(entry.getWord());
    deAdjectiveCount++;
  }
  System.out.println("German adjectives: " + deAdjectiveCount);
  wkt.close();
```


## Extracting sense definitions, examples, and quotations ##

The meaning of a word sense is described in a sense definition giving a brief paraphrase or usage note of a word sense (often called a "gloss"). Code example:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense sense = entry.getSense(1);
  System.out.println(sense.getGloss().getText());
  wkt.close();
```

The sense definition is encoded using the Wiki markup language. JWKTL represents such strings using the `IWikiString` interface. This facilitates accessing the original text including all markup (`getText()`) and accessing a more reader-friendly version without most of the wiki markup (`getPlainText()`).

In addition to that, the usage of a word sense might be illustrated by example sentences or quotations. Note that for the sake of memory consumption, many information types are set to `null` if no Wiktionary information is provided for this article position. Remember to check for `null` if the information type might be missing. Code example:


```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense sense = entry.getSense(1);

  // Example sentences.
  if (sense.getExamples() != null)
    for (IWikiString example : sense.getExamples())
      System.out.println(example.getText());

  // Quotations.
  if (sense.getQuotations() != null)
    for (IQuotation quotation : sense.getQuotations()) {
      for (IWikiString line : quotation.getLines())
        System.out.println(line.getText());
      System.out.println("--" + quotation.getSource().getText());
    }

  wkt.close();
```


## Extracting semantic relations ##

Two words (word senses) can be related in their meaning, for example, by describing the same or an opposite meaning. This is modeled by what is called semantic relations pointing from one word sense to a certain target article. The following example code shows how to extract all synonyms of the noun _boat_:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  for (IWiktionaryEntry entry : page.getEntries())
    if (entry.getPartOfSpeech() == PartOfSpeech.NOUN)
     for (IWiktionaryRelation relation : entry.getRelations(RelationType.SYNONYM))
       System.out.println(relation.getTarget());
  wkt.close();
```

Accordingly, all semantic relations can be extracted:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  for (IWiktionaryEntry entry : page.getEntries())
    if (entry.getPartOfSpeech() == PartOfSpeech.NOUN)
      for (IWiktionaryRelation relation : entry.getRelations())
        System.out.println(relation.getRelationType() + ": " + relation.getTarget());
  wkt.close();
```

So far, we have accessed all semantic relations of a lexical entry regardless of the word senses. This is achieved by selecting the `IWiktionarySense` in question and essentially invoking the same methods as for the `IWiktionaryEntry`:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense sense = entry.getSense(1);
  for (IWiktionaryRelation relation : sense.getRelations(RelationType.SYNONYM))
    System.out.println(relation.getTarget());
  wkt.close();
```

Semantic relations that could not be associated to a specific word sense are gathered within the unassigned sense:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense unassigned = entry.getUnassignedSense();
  for (IWiktionaryRelation relation : unassigned.getRelations(RelationType.SYNONYM))
    System.out.println(relation.getTarget());
  wkt.close();
```



## Extracting translations ##

Wiktionary encodes a large number of translations, which are represented as a hyperlink from one article to another. The German translation of the English word _boat_ can, for instance, be accessed using the following code:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  for (IWiktionaryEntry entry : page.getEntries())
    if (entry.getPartOfSpeech() == PartOfSpeech.NOUN)
      for (IWiktionaryTranslation translation : entry.getTranslations(Language.GERMAN))
        System.out.println(translation.getTranslation());
  wkt.close();
```

Accordingly, it is easy to print a list of all translations encoded for the noun _boat_:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  for (IWiktionaryEntry entry : page.getEntries())
    if (entry.getPartOfSpeech() == PartOfSpeech.NOUN)
      for (IWiktionaryTranslation translation : entry.getTranslations())
        System.out.println(translation.getLanguage() + ": " + translation.getTranslation());
  wkt.close();
```

For the semantic relations, we have seen that many of them are associated with a certain word sense. This also applies to the translations. The following sample code extracts the German translations of the first word sense of _boat_ (i.e., the meaning of a water vessel):

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense sense = entry.getSense(1);
  for (IWiktionaryTranslation translation : sense.getTranslations(Language.GERMAN))
      System.out.println(translation.getTranslation());
  wkt.close();
```

Again, translations that could not be associated to a specific word sense are gathered within the unassigned sense:

```
  IWiktionaryEdition wkt = JWKTL.openEdition(WIKTIONARY_DIRECTORY);
  IWiktionaryPage page = wkt.getPageForWord("boat");
  IWiktionaryEntry entry = page.getEntry(0);
  IWiktionarySense unassigned = entry.getUnassignedSense();
  for (IWiktionaryTranslation translation : unassigned.getTranslations(Language.GERMAN))
      System.out.println(translation.getTranslation());
  wkt.close();
```


## Multilingual processing ##

JWKTL allows processing and combining information from multiple Wiktionary language editions using the same code and API. That is to say, An `IWiktionaryCollection` may be created which is basically a list of `IWiktionaryEdition`s. The collection offers largely the same methods for querying and iterating the encoded information. The following code example extracts, for instance, all entries for the word form <i>arm</i> encoded in the two given Wiktionary databases (e,g., the English and German false friends):

```
  IWiktionaryCollection wktColl = JWKTL.openCollection(WIKTIONARY_DIRECTORY1, WIKTIONARY_DIRECTORY2);
  for (IWiktionaryEntry entry : wktColl.getEntriesForWord("arm")) {
    // Print the language of the defining language edition.
    System.out.println(entry.getPage().getEntryLanguage() + ":");

    // Print the word and its language and part of speech.
    System.out.println("  " + entry.getWord() 
        + "/" + entry.getPartOfSpeech() 
        + "/" + entry.getWordLanguage());
  }
  wkt.close();
```

Remember that each Wiktionary language edition encodes entries from multiple different languages. Use `getEntryLanguage()` to identify the language of the language edition encoding an entry (e.g., the English Wiktionary - this is the language used for describing the lexicographic information), and use `getWordLanguage()` to obtain the language of the actual word (e.g., a German noun). Take a look at our publications for more information on this distinction.