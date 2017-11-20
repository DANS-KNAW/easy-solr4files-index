easy-update-solr4files-index
============================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-update-solr4files-index.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-update-solr4files-index)

* [SYNOPSIS](#synopsis)
  + [HTTP service](#http-service)
* [DESCRIPTION](#description)
* [ARGUMENTS](#arguments)
* [EXAMPLES](#examples)
* [INSTALLATION AND CONFIGURATION](#installation-and-configuration)
  + [Prerequisites](#prerequisites)
  + [steps](#steps)
  + [Security advice](#security-advice)
* [BUILDING FROM SOURCE](#building-from-source)


SYNOPSIS
--------

    easy-update-solr4files-index update [-s <bag-store>] <uuid>
    easy-update-solr4files-index init <bag-store>
    easy-update-solr4files-index run-service
    easy-update-solr4files-index delete <solr-query>
    easy-update-solr4files-index run-service
    
    Some examples of standard solr queries for the delete command:
    
      everything:            '*:*'
      all bags of one store: 'easy_dataset_store_id:pdbs'
      a bag:                 'easy_dataset_id:ef425828-e4ae-4d58-bf6a-c89cd46df61c'
      a folder in a bag:     'id:ef425828-e4ae-4d58-bf6a-c89cd46df61c/data/files/Documents/*'


### HTTP service

When started with the sub-command `run-service` a REST API becomes available summarized in the following table.
"Method" refers to the HTTP method used in the request. "Path" is the path pattern used. 
Placeholders for variables start with a colon, optional parts are enclosed in square brackets.

Method   | Path                             | Action
---------|----------------------------------|------------------------------------
`GET`    | `/fileindex`                     | Return a simple message to indicate that the service is up: "EASY File index is running."
`POST`   | `/fileindex/init[/:store]`       | Index all bag stores or just one. Eventual obsolete items are cleared.
`POST`   | `/fileindex/update/:store/:uuid` | Index all files of one bag. Eventual obsolete file items are cleared.
`DELETE` | `/fileindex/:store[/:uuid]`      | Remove all items from the index or the items of a store or bag.
`DELETE` | `/fileindex/`                    | Requires parameter q, a mandatory [standard] solr query that specifies the items to remove from the index.
`GET`    | `/filesearch`                    | Return indexed metadata. Query parameters are optional, not known parameters are ignored.


Parameters for `filesearch` | Description
----------------------------|----------------
`text`                      | The query for the textual content. Becomes the `q` parameter of a [dismax] query. If not specified all accessible items are returned unless a restriction is specified.
`skip`                      | For result paging, default 0.
`limit`                     | For result paging, default 10.
`dataset_id`, `dataset_doi` | Restrict to one or some datasets. Repeating just one type of the identifiers returns items for each value. Mixing identifier types only returns items matching at least one of the values for each type.
`dataset_depositor_id`      | Restrict to the specific dataset field. Repeating a field returns items with at least one of the values. Specifying multiple fields only returns items matching at least one of the values for each field.
`file_mime_type`            | ,,
`file_size`                 | ,,
`file_checksum`             | ,,
`dataset_title`             | ,,
`dataset_creator`           | ,,
`dataset_audience`          | ,,
`dataset_relation`          | ,,
`dataset_subject`           | ,,
`dataset_coverage`          | ,,

[dismax]: https://lucene.apache.org/solr/guide/6_6/the-dismax-query-parser.html#the-dismax-query-parser
[standard]: https://lucene.apache.org/solr/guide/6_6/the-standard-query-parser.html


DESCRIPTION
-----------

Update the EASY SOLR for Files Index with file data from a bag-store.

File content is indexed together with some file metadata as well as dataset metadata.
Files are only indexed if `easy_file_accessible_to` gets a value 
`anonymous`, `known`, `restrictedGroup` or `restrictedRequest`.
If the value is not provided at file level by `metadata/files.xml`,
a default is derived from `<ddm:profile><ddm:accessRights>` in `metadata/dataset.xml`.


ARGUMENTS
---------

    Options:

      --help      Show help message
      --version   Show version of this program

    Subcommand: update - Update accessible files of a bag in the SOLR index
      -s, --bag-store  <arg>   Name of the bag store (default = pdbs)
          --help               Show help message
    
     trailing arguments:
      bag-uuid (required)
    ---
    
    Subcommand: delete - Delete documents from the SOLR index
          --help   Show help message
    
     trailing arguments:
      solr-query (required)
    ---
    
    Subcommand: init - Rebuild the SOLR index from scratch for active bags in one or all store(s)
          --help   Show help message
    
     trailing arguments:
      bag-store (not required)
    ---
    
    Subcommand: run-service - Starts EASY Update Solr4files Index as a daemon that services HTTP requests
          --help   Show help message
    ---

EXAMPLES
--------

Using the command line to update a single bag in the store `pdbs` respective (re)index all bags in all stores.

    easy-update-solr4files-index -s pdbs update 9da0541a-d2c8-432e-8129-979a9830b427
    easy-update-solr4files-index init

Using the rest interface to delete a bag from the index respective (re)index all bags in one store.

    curl -X DELETE 'http://test.dans.knaw.nl:20150/fileindex/?q=easy_dataset_id:ef425828-e4ae-4d58-bf6a-c89cd46df61c'
    curl -X POST 'http://test.dans.knaw.nl:20150/fileindex/init/pdbs'

Retrieve the second page of PDF and text files containing one of the words `foo` or `bar`.
See the [security advice](#security-advice) for the URL part preceding the `?`.

    curl 'http://test.dans.knaw.nl:20150/filesearch?text=foo+bar&file_mime_type=application/pdf&file_mime_type=text&skip=10&limit=10'


INSTALLATION AND CONFIGURATION
------------------------------

### Prerequisites

* [easy-bag-store](https://github.com/DANS-KNAW/easy-bag-store/)
* [dans.solr](https://github.com/DANS-KNAW/dans.solr)
* [dans.easy-ldap-dir](https://github.com/DANS-KNAW/dans.easy-ldap-dir)
* A [Solr core](src/main/assembly/dist/install/fileitems),
  installed for example with with [vagrant.yml](src/main/ansible/vagrant.yml).
  Thus a web-ui comes available for administrators with `http://localhost:8983/solr/#/fileitems/query`.
  A command line example:

        curl 'http://test.dans.knaw.nl:8983/solr/fileitems/query?q=*&fl=*'

### Steps

1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-update-solr4files-index-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-update-solr4files-index-<version>/bin/easy-update-solr4files-index /usr/bin

General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


### Security advice

Keep the admin interface (command line and `fileindex` servlet)
and any other direct access to solr, the bag store and ldap behind a firewall.
Only expose the `filesearch` servlet through a proxy, map for example:
`http://easy.dans.knaw.nl/files/search` to `http://localhost:20150/filesearch` 


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-update-solr4files-index.git
        cd easy-update-solr4files-index
        mvn install
