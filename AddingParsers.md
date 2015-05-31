# Adding new parsers to JWKTL #

The different language editions of Wiktionary differ in the way they encode the lexicographic information. To get in a position to access data from language editions which are not yet handled by JWKTL, you can add a new parser.

Imagine, you want to add a parser for French. Take a look at the `parser.*` packages first. This package contains all classes you need to get your new parser working. Create a `parser.fr.FRWiktionaryEntryParser` extending the generic `WiktionaryEntryParser` and register your class in `WiktionaryArticleParser.resolveLanguage()`. This should allow you to start parsing of a French Wiktionary dump file as discussed in the [getting started](GettingStarted.md) guide.

Of course, this would not extract most of the data yet. The "heart" of a parser is its components and handlers. That is, small components which are able to extract a specific article constituent. The articles of the French Wiktionary contain, for example, a headline "Étymologie" under which the etymology of a word is described. One handler could thus be an `FREtymologyHandler` which asks for being invoked whenever "Étymologie" is read, and which extracts the subsumed etymological descriptions. Register the handler at the `FRWiktionaryEntryParser` and start the parser.

Though this is just a very general descriptions, you should get a general idea of how JWKTL parsers work.


## Parser types ##

You might wonder what the different types of parsers are used for. Here's a brief overview:

  * The `parser.XMLDumpParser` class is basically a SAX parser wrapper. It maintains a stack of tags in the current hierarchy and provides the means to access the attributes and CDATA of the tags. Little magic is going on here and you should not require making any changes or invoking a method from this class - despite the `parse()` method of course which is used to start the parser.
  * `parser.WiktionaryDumpParser` extends the `XMLDumpParser`. It expects the XML schema used in Wiktionary XML dumps. That is, it listens for any tag read by the `XMLDumpParser`, determines the tag name and invokes an abstract hotspot for the specific tags. There is, for example, a method `setTitle(String)`, which is called upon reading `<title>…</title>` from the XML dump. Again little magic and no need for changing or invoking something.
  * Next is `parser.WiktionaryPageParser` which extends the `WiktionaryDumpParser` (it is however planned to remove this inheritance relation in order to support multiple `WiktionaryPageParsers` listening on a single `WiktionaryDumpParser`). The page parser implements the hotspots of the `DumpParser` and fills an instance of type `WiktionaryPage`, i.e. an instance that allows accessing the metadata of a wiki page including title, author, ID, revision, date of last change, etc. (see `IWiktionaryPage`) - but this class does not handle the `<text>` tag yet.
  * Finally, `parser.WiktionaryArticleParser` is the last subclass in this hierarchy. It extends the `WiktionaryPageParser` and has two responsibilities: it cares about storing completely parsed pages in the BerkeleyDB and it manages a `WiktionaryEntryParser` to which it passes the text of the wiki page.
  * The heart of each wiki page is of course the page’s text. This is why JWKTL has a separate class, `parser.WiktionaryEntryParser`, for extracting the different information items from the text and fill the `WiktionaryPage` (created by the `WiktionaryPageParser`) with `WiktionaryEntry`s and `WiktionarySense`s. The entry parser class contains the parser method which invokes the different handlers. It has also language-specific subclasses that configure the entry parser by attaching the corresponding handlers of each


## Entry parser main loop ##

The entry parser loop works as follows:

  * The wiki text is read line-by-line.
  * The parser can be in one of two states: HEAD and BODY. It starts in the HEAD state.
  * As long as there are more lines to read:
    * If we are in the HEAD state:
      * check if the current line represents the beginning of a block (e.g., etymology, pronunciation, translations, etc.). The beginning of a block is usually indicated by a headline format (e.g., ===Translations===) or by a template (e.g., {{en-noun}}). So if the line is a potential beginning of a block, iterate through all attached handlers and ask them if they want to handle this block. A translation handler would, for instance, check if the block has a headline “Translations” and then return true to notify the entry parser that it wants to handler this block. Otherwise it returns false.
        * The corresponding check of the handler is being done within the canHandle() method of each handler
        * There is, however, a `FixedNameBlockHandler` which provides a generic implementation checking for static headlines (such as “translation”) – in this case you only need to specify the static labels/headlines of a block in the constructor. Other handlers such as the English Wiktionary’s part of speech handler do not have a static headline but begin with a template like {{en-noun}} or {{en-verb}}, etc. – in this case you need to write a separate `canHandle()` method
      * If we found a handler that is willing to handle the current line, save it - otherwise the handler is null (this happens for blocks which are not yet implemented, e.g., the “Anagrams” section of the English Wiktioanry.
      * If we have an active handler, invoke its `processHead()` method. This allows the handler to process the current line, which is useful for the part of speech handler discussed above – it parses the {{en-noun}} etc. templates and saves the part of speech tag.
      * Some blocks consist of only one line (e.g., the language of a word) Such handlers return false in its `processHead()` method to indicate that the next line may be handled by another handler. They are thus immediately finished. Other blocks (e.g., the list of translations) require the processing of multiple subsequent lines, which we call the BODY. Therefore, the entry parser switches to the BODY state if the handler’s `processHead()` method returns true.
      * Then, the next line of text is being read and the parser starts again with processing its main loop
    * If we are in the BODY state:
      * If we still have a handler that is willing to process lines, invoke its `processBody()` method. The translation handler can, for instance, parse the translation template contained in this line and save the extracted translation to some internal list.
      * Some handlers require a certain number of lines – if you read for example a table which spans over multiple lines, you want the handler to process each line until the table is being closed again. In this cases, the handler’s `processBody()` method should return true. In this case, the next line is being read and the `processBody()` method of this handler is being called with this next line.
      * If the handler is, however, willing to pass on to the next handler (if one is willing to do so), it should return false.
      * In such cases (the handler tells the entry parser that it is willing to pass on), the next line being read by the entry parser is checked for being the beginning of a block. If so, the entry parser invokes the `fillContent()` method of the handler which tells the handler that it will not be invoked again in the current block and that it should save all extracted data to the Wiktionary data objects (i.e., `WiktionaryPage`, `WiktioanryEntry`, and `WiktionarySense`). The parser then changes into the HEAD state and starts over again with the main loop.
      * If the parser is willing to pass on, but the next line does NOT denote the beginning of a block, the entry parser invokes again the `processBody()` method of the current handler, as the current line still belongs to the current block (the translation handler would thereby receive another translation to process).