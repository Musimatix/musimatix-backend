domain Common.Russian.Prosody

type Phoneme {
    introduction {
        feature notation {
            viewname {
                Запись фонемы
            }
            description {}
        }
    }
    hierarchy standard {
        openset feature notation;
    }
}

type Syllable {
    introduction {
        Integer feature accent {
            viewname {
                Ударность
            }
            description {}
            value 0 {
              viewname {}
              description {}
            }
            value 1 {
              viewname {}
              description {}
            }
            value 2 {
              viewname {}
              description {}
            }
        }
    }
    hierarchy standard {
        feature accent {
            value 0;
            value 1;
            value 2;
        }
    }
}

type AccVariant {
    introduction {
        Treenotation feature MorphArr {
            viewname {}
            description {}
        }

        Integer feature dolnikVer {
            viewname {}
            description {}
        }
        Integer feature iambusVer {
            viewname {}
            description {}
        }
        Integer feature trocheusVer {
            viewname {}
            description {}
        }
        Integer feature amphibracheusVer {
            viewname {}
            description {}
        }
        Integer feature dactilusVer {
            viewname {}
            description {}
        }
        Integer feature anapaestusVer {
            viewname {}
            description {}
        }
        Integer feature stats {
            viewname {}
            description {}
        }
        Boolean feature userVariant {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}

type Fragment {           
    introduction {
        feature type {
            viewname {}
            description {}
            value strophe {
              viewname {}
              description {}
            }
            value chapter {
              viewname {}
              description {}
            }
            value piece_of_art {
              viewname {}
              description {}
            }
        }
        feature caption {
            viewname {}
            description {}
        }
        Boolean feature displayText {
            viewname {}
            description {}
        }
    }
    hierarchy standard {
        openset feature caption;
        feature type {
            value strophe;
            value chapter;
            value piece_of_art;
        }
        openset feature displayText;
    }
}

type Split {
    introduction {
        feature name {
            viewname {}
            description {}
        }
        feature type {
            viewname {}
            description {}
        }
    }
    hierarchy standard {
        openset feature name;
        openset feature type;
    }
}

type Verse {
    introduction {
        feature meter {
            viewname {}
            description {}
            value dolnik {
                viewname {}
                description {}
            }
            value iambus {
                viewname {}
                description {}
            }
            value trocheus {
                viewname {}
                description {}
            }
            value amphibracheus {
                viewname {}
                description {}
            }
            value dactilus {
                viewname {}
                description {}
            }
            value anapaestus {
                viewname {}
                description {}
            }
            value unknown {
                viewname {}
                description {}
            }
        }
        Integer feature dolnikVer {
            viewname {}
            description {}
        }
        Integer feature iambusVer {
            viewname {}
            description {}
        }
        Integer feature trocheusVer {
            viewname {}
            description {}
        }
        Integer feature amphibracheusVer {
            viewname {}
            description {}
        }
        Integer feature dactilusVer {
            viewname {}
            description {}
        }
        Integer feature anapaestusVer {
            viewname {}
            description {}
        }
        Integer feature nVariants {
            viewname {}
            description {}
        }
        Integer feature stats {
            viewname {}
            description {}
        }
        feature form {
            viewname {}
            description {}
            value I {
                viewname {}
                description {}
            }
            value II {
                viewname {}
                description {}
            }
            value III {
                viewname {}
                description {}
            }
            value IV {
                viewname {}
                description {}
            }
            value V {
                viewname {}
                description {}
            }
            value VI {
                viewname {}
                description {}
            }
            value VII {
                viewname {}
                description {}
            }
            value VIII {
                viewname {}
                description {}
            }
            value unknown {
                viewname {}
                description {}
            }
            value ambig {
                viewname {}
                description {}
            }
        }
        Treenotation feature AccentVariantArr {
            viewname {}
            description {}
        }
    }
    hierarchy standard {
        openset feature nVariants;
        openset feature AccentVariantArr;
        openset feature stats;
        openset feature meter {
            value dolnik;
            value iambus {
                optional feature form {
                    value I;
                    value II;
                    value III;
                    value IV;
                    value V;
                    value VI;
                    value VII;
                    value VIII;
                    value unknown;
                    value ambig;
                }
            }
            value trocheus;
            value amphibracheus;
            value dactilus;
            value anapaestus;
            value unknown {
                optional feature form {
                    value unknown;
                }
            }
        }
    }
}

type PhonWord {
    introduction {}
    hierarchy standard {}
}

type CorpusElement {           
    introduction {
        feature author {
            viewname {}
            description {}
        }
        feature title {
            viewname {}
            description {}
        }
        Integer feature year {
            viewname {}
            description {}
        }
	feature meter {
            viewname {}
            description {}
        }
	feature cycle {
            viewname {}
            description {}
        }
    }
    hierarchy standard {
        openset feature author;
        openset feature title;
        openset feature year;
        openset feature meter;
        openset feature cycle;
    }
}

type IgnoredText {           
    introduction {}
    hierarchy standard {}
}