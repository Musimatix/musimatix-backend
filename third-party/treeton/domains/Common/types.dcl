domain Common

tokentype Atom {
    introduction {}
    hierarchy standard {}
}

type System {
    introduction {
        feature name {
            viewname {}
            description {}
        }
        feature mark {
            viewname {}
            description {}
        }
        feature autocreated {
            viewname {}
            description {}
        }
        feature fragment {
            viewname {}
            description {}
        }
        feature penalty {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}

type RuleUsage {
    introduction {
        feature rule {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}

type DEFAULT_TOKEN {
    introduction {}
    hierarchy standard {}
}

type Token {
    introduction {
        feature kind {
            viewname {
                Тип токена
            }
            description {}
            value punctuation {
                viewname {
                    Знак пунктуации
                }
                description{
                    Символы Unicode типа: DASH_PUNCTUATION, CONNECTOR_PUNCTUATION,
                    OTHER_PUNCTUATION, START_PUNCTUATION, INITIAL_QUOTE_PUNCTUATION,
                    END_PUNCTUATION, FINAL_QUOTE_PUNCTUATION
                }
            }
            value symbol {
                viewname {
                    Символ
                }
                description{
                    Символы Unicode типа: MODIFIER_SYMBOL, MATH_SYMBOL, OTHER_SYMBOL,
                    CURRENCY_SYMBOL
                }
            }
            value word {
                viewname {
                    Слово
                }
                description{
                    Последовательность букв любого регистра
                }
            }
            value number {
                viewname {
                    Число
                }
                description {
                    Последовательность цифр
                }
            }
        }
        feature subkind {
            viewname {
                Подтип токена
            }
            description {
                Появляется только у знаков пунктуации
            }
            value dashpunct {
                viewname {
                    Дефисы
                }
                description{
                    Символы Unicode типа: DASH_PUNCTUATION
                }
            }
        }
        feature position {
            viewname {
                Позиция парного знака (например скобки)
            }
            description {
                Либо открывающий, либо закрывающий знак
            }
            value startpunct {
                viewname {
                    Открывающий знак (например левая скобка)
                }
                description{}
            }
            value endpunct {
                viewname {
                    Закрывающий знак (например правая скобая)
                }
                description{}
            }
        }
        feature lang {
            viewname {
                Алфавит цепочки
            }
            description {
                Для токенов, у которых kind=word, значением этого признака является алфавит, которому принадлежат
                символы данной цепочки.
            }
            value lat {
                viewname {
                    Латинский алфавит
                }
                description{}
            }
            value cyr {
                viewname {
                    Кириллический алфавит
                }
                description{}
            }
            value mix {
                viewname {
                    Смешанный алфавит
                }
                description {
                    Смесь кириллицы и латиницы
                }
            }
            value undefined {
                viewname {
                    Неясный алфавит
                }
                description {}
            }
        }
        feature orth {
            viewname { Вхождение прописных букв}
            description { Характеристика цепочки с точки зрения наличия в неё прописных букв }
            value lowercase {
                viewname { Все буквы строчные }
                description { Все буквы строчные }
            }
            value upperInitial {
                viewname { Первая буква - прописная }
                description { Первая буква прописная, остальные строчные }
            }
            value allCaps {
                viewname { Все буквы прописные }
                description { Все буквы прописные  }
            }
            value mixedCaps {
                viewname { Имеются и строчные и прописные буквы }
                description { Имеются и строчные и прописные буквы }
            }
            value upperInitialWithMixedCaps {
                viewname {}
                description{}
            }
        }
        feature charset {
            viewname {}
            description {}
            value cyr {
                viewname {}
                description{}
            }
            value lat {
                viewname {}
                description{}
            }
            value other {
                viewname {}
                description{}
            }
        }
        feature symbolkind {
            viewname {}
            description {}
            value currency {
                viewname {
                    Символ валюты
                }
                description{
                    Символы Unicode типа: CURRENCY_SYMBOL
                }
            }
        }
    }

    hierarchy standard {
        feature kind {
            mark sys;
            value punctuation {
                optional feature subkind {
                    mark sys;
                    value dashpunct;
                }
                optional feature position {
                    mark sys;
                    value startpunct;
                    value endpunct;
                }

            }
            value symbol {
                optional feature symbolkind {
                    mark sys;
                    value currency;
                }
            }
            value word {
                feature lang {
                    mark sys;
                    value undefined;
                    value lat;
                    value cyr;
                    value mix;
                }
                feature orth {
                    mark sys;
                    value lowercase;
                    value upperInitial;
                    value allCaps;
                    value mixedCaps;
                    value upperInitialWithMixedCaps;
                }
                optional feature charset {
                    mark sys;
                    value cyr;
                    value lat;
                    value other;
                }
            }
            value number;
        }
    }
}

type SpaceToken {
    introduction {
        feature kind {
            viewname {
                Тип токена
            }
            description {}
            value space {
                viewname {
                    Пробел
                }
                description {}
            }
            value control {
                viewname {
                    Перенос строки
                }
                description {}
            }
        }
    }
    hierarchy standard {
        feature kind {
            value space;
            value control;
        }
    }
}



type Sem {
    introduction {
        feature name {
            viewname {}
            description {}
        }
        feature semanticSlot {
            viewname {}
            description {}
        }

        feature LexicalClass0 {
            viewname {}
            description {}
        }
        feature Value0 {
            viewname {}
            description {}
        }
        feature LexicalClass1 {
            viewname {}
            description {}
        }
        feature Value1 {
            viewname {}
            description {}
        }
        feature LexicalClass2 {
            viewname {}
            description {}
        }
        feature Value2 {
            viewname {}
            description {}
        }
        feature IsSubstitutedProform {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}

type Lexeme {
    introduction {
        feature name {
            viewname {}
            description {}
        }
        feature semanticSlot {
            viewname {}
            description {}
        }
        feature LexicalClass0 {
            viewname {}
            description {}
        }
        feature Value0 {
            viewname {}
            description {}
        }
        feature LexicalClass1 {
            viewname {}
            description {}
        }
        feature Value1 {
            viewname {}
            description {}
        }
        feature LexicalClass2 {
            viewname {}
            description {}
        }
        feature Value2 {
            viewname {}
            description {}
        }
        feature IsSubstitutedProform {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}
