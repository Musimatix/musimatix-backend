domain Common.Russian

type Gramm extends Token {
    introduction {
        feature TYPE {
            viewname {}
            description {}
        }

        feature SYNTTYPE {
            viewname {}
            description {}
        }

        feature kind {
            viewname {}
            description {}
            value complex {
                viewname {
                    Сложное образование
                }
                description {
                    Например, "г/номер". Это никогда не будет одним токеном, но т.к.
                    словарные Morph-ы "наследуют" токены и в них должны быть токеновские фичи,
                    там такое может появится.
                }
            }
        }
        feature base {
            viewname {
                Исходная форма
            }
            description {}
        }

        feature ANALOG {
            viewname {
                Для слов, отсутствующих в словаре, - начальная форма словарного слова,
		на основе которого построена гипотеза.
            }
            description {}
        }

        feature ipm {
            viewname {
		instances per million words
            }
            description {}
        }	

        feature GOV {
            viewname {
                Описание модели управления
            }
            description {}
        }

        feature GOVID {
            viewname {
                Ссылка на модель управления
            }
            description {}
        }

        feature SEM {
            viewname {
                Значение
            }
            description {}
        }


        feature AUX {
            viewname {
                Признак лексем из грязных словарей
            }
            description {}
            value aux {
                viewname {}
                description {}
            }
        }

        feature WORDFORM {
            viewname {
                Нормализованное исходное слово
            }
            description {
                Исходное слово, в котором регистры букв соответствуют значению шаблона
                freqOrthm
            }
        }
        feature freqOrthm {
            viewname {
                Канонический способ написания
            }
            description {
                Маска слова, представляющая собой цепочку символов, в которой символ u
                соотвествует заглавной букве, l - маленькой букве, * - букве любого
                регистра. Кроме того, в маске могут присутствовать конкретные буквы
                русского алфавита. Значение последнего символа в маске распространяется
                на все последующие символы в слове.
            }
        }
        feature ZINDEX {
            viewname {
                Индекс Зализняка
            }
            description {}
        }
        feature ENDDOT {
            viewname {
                Способность лексемы "поглощать" точку после себя
            }
            description {
                Способность лексемы "поглощать" точку после себя. Атрибут проставляется у сокращений типа
                "апр." или "г.". Трактовать эту способность нужно как некую потенцию. Если в тексте встретится
                соответствующая лексема без конечно точки, она все ранво должна накрываться.
            }
            value enddot {
                viewname {}
                description {}
            }
        }
        Integer feature ACCPL {
            viewname {
                Место ударения
            }
            description {
                Порядковый номер буквы в орфографическом написании (нумерация с 1)
            }
        }
        Integer feature ACCSECPL {
            viewname {
                Место вторичного ударения
            }
            description {
                Порядковый номер буквы в орфографическом написании (нумерация с 1)
            }
        }
        Integer feature ACCYOPL {
            viewname {
                Позиция буквы ё
            }
            description {
                Порядковый номер буквы в орфографическом написании (нумерация с 1)
            }
        }
        Integer feature DICTID {
            viewname {
                Идентификатор морфологического словаря
            }
            description {
                Идентификатор морфологического словаря, в котором хранится данная лексема
            }
        }
        Integer feature ID {
            viewname {
                Идентификатор лексемы в словаре
            }
            description {}
        }
        feature IRREG {
            viewname {
                Нерегулярность словоизменения
            }
            description {
                Лексемы, парадигмы которых полностью прописываются в файле spec_ru.dct
            }
            value irreg {
                viewname {}
                description {}
            }
        }
        feature NOTATION {
            viewname {
                Нотация
            }
            description {
                Способ написания слова. На данный момент имеется только одно значение: цифровая нотация.
                Если в записи слова не участвуют цифры, то этот атрибут вообще не появляется.
            }
            value numerical {
                viewname {
                    Цифровая нотация
                }
                description {
                    Появляется только у прилагательных или числительных, в записи которых участвуют цифры.
                    Например, выражения типа: 1-я; 37-ми; 23; 56; 0.75; 0,5
                }
            }
        }
        feature ANOM {
            viewname {
                Неканоничное написание исходной формы
            }
            description {}
            value anom {
                viewname {}
                description {}
            }
        }
        feature POS {
            viewname {
                Часть речи
            }
            description {
            }
            value N {
                viewname {
                    Существительное
                }
                description {}
            }
            value A {
                viewname {
                    Прилагательное
                }
                description {}
            }

            value NUM {
                viewname {
                    Числительное
                }
                description {}
            }
            value V {
                viewname {
                    Глагол
                }
                description {}
            }
            value PREP {
                viewname {
                    Предлог
                }
                description {}
            }
            value ADV {
                viewname {
                    Наречие
                }
                description {}
            }
            value PRED {
                viewname {
                    Предикатив
                }
                description {}
            }
            value PAR {
                viewname {
                    Вводное слово
                }
                description {}
            }
            value CONJ {
                viewname {
                    Союз
                }
                description {}
            }
            value INTJ {
                viewname {
                    Междометие
                }
                description {}
            }
            value PCL {
                viewname {
                    Частица
                }
                description {}
            }
        }

        feature HASGEND {
            viewname {
                Изменяется по родам
            }
            description {
                для числительных
            }
            value hasgend {
                viewname {}
                description {}
            }
        }

        feature INVAR {
            viewname {
                Неизменяемое слово
            }
            description {}
            value invar {
                viewname {}
                description {}
            }
        }

        feature LEXTYPE {
            viewname {
                Лексический тип
            }
            description {
                Появляется у имен собственных и выражений
            }
            value propnoun {
                viewname {
                    Имя собственное
                }
                description {}
            }
        }

        feature PNT {
            viewname {
                Тип имени собственного
            }
            description {}
            value fam {
                viewname {
                    Фамилия
                }
                description {}
            }
            value fnam {
                viewname {
                    Личное имя
                }
                description {}
            }
            value ptrn {
                viewname {
                    Отчество
                }
                description {}
            }
        }

        feature HPC {
            viewname {
                Гипокористичность
            }
            description {
                Уменьшительно-ласкательное имя
            }
            value hpc {
                viewname {
                    Гипокористическое (уменьшительно-ласкательное) имя
                }
                description {}
            }
        }

        feature Klitik {
            viewname {
                Признак клитики
            }
            description {
                Признак клитики
            }
            value klitik {
                viewname {
                    Признак клитики
                }
                description {}
            }
        }

        feature OPGEND {
            viewname {
                Допустимость противоположного рода
            }
            description {
                Для первых имен
            }
            value opgend {
                viewname {}
                description {}
            }
        }

        feature ADJI {
            viewname {
                Существительное, изменяющееся по адъективному склонению
            }
            description {}
            value adji {
                viewname {
                    Адъективное склонение
                }
                description {
                    Портной, жаркое, суточные
                }
            }
        }

        feature PRN {
            viewname {
                Местоименность
            }
            description {
            }
            value prn {
                viewname {
                    Местоименное
                }
                description {
                    Который, каждый, где, там
                }
            }
        }

        feature IREL {
            viewname {
                Вопросительно-относительное местоимение
            }
            description {
                кто, какой, где,...
            }
            value irel {
                viewname {
                    Вопросительно-относительное местоимение
                }
                description {}
            }
        }

        feature ADPREP {
            viewname {
                Припредложность местоимения
            }
            description {}
            value adprep {
                viewname {
                    Форма припредложного местоимения с начальной буквой "н"
                }
                description {}
            }
        }

        feature ANIM {
            viewname {
                Одушевленность
            }
            description {}
            value anim {
                viewname {
                    Одушевленное
                }
                description {}
            }
            value inan {
                viewname {
                    Неодушевленное
                }
                description {}
            }
        }

        feature GEND {
            viewname {
                Род
            }
            description {}
            value m {
                viewname {
                    Мужской
                }
                description {}
            }
            value f {
                viewname {
                    Женский
                }
                description {}
            }
            value n {
                viewname {
                    Средний
                }
                description {}
            }
            value mf {
                viewname {
                    Общий (мужской или женский)
                }
                description {
                    Забияка, зануда
                }
            }
            value mn {
                viewname {
                    Мужской или средний
                }
                description {
                    Для местоимений "его", "ему" и т. п.
                }
            }
            value mfn {
                viewname {
                    Мужской или женский или средний
                }
                description {
                    Для местоимений "я" и "ты"
                }
            }
            value plt {
                viewname {
                    Только множественное число (plurale tantum)
                }
                description {
                    Санки, ножницы
                }
            }
        }

        feature NMB {
            viewname {
                Число
            }
            description {}
            value sg {
                viewname {
                    Единственное
                }
                description {}
            }
            value pl {
                viewname {
                    Множественное
                }
                description {}
            }
        }

        feature CAS {
            viewname {
                Падеж
            }
            description {}
            value nom {
                viewname {
                    Номинатив (именительный)
                }
                description {}
            }
            value gen {
                viewname {
                    Генитив (родительный)
                }
                description {}
            }
            value gen2 {
                viewname {
                    Второй генитив (второй родительный)
                }
                description {
                    "Дай мне чаю"
                }
            }
            value dat {
                viewname {
                    Датив (дательный)
                }
                description {}
            }
            value acc {
                viewname {
                    Аккузатив (винительный)
                }
                description {}
            }
            value inst {
                viewname {
                    Инструменталис (творительный)
                }
                description {}
            }
            value prp {
                viewname {
                    Препозиционалис (предложный)
                }
                description {}
            }
            value prp2 {
                viewname {
                    Второй препозиционалис (второй предложный)
                }
                description {
                    "Работать в саду"
                }
            }
            value voc {
                viewname {
                    Вокатив (звательный)
                }
                description {
                    Мам, пап, тёть
                }
            }
        }

        feature ORDIN {
            viewname {
                Порядковость прилагательного
            }
            description {}
            value ordin {
                viewname {
                    Порядковое прилагательное
                }
                description {
                    Первый, девятнадцатый, 19-й
                }
            }
        }

        feature DGR {
            viewname {
                Степень сравнения прилагательного
            }
            description {}
            value comp {
                viewname {
                    Сравнительная
                }
                description {
                    Петя умнее вани.
                }
            }
        }

        feature ATTR {
            viewname {
                Атрибутивность прилагательного
            }
            description {
                Полная/краткая форма
            }
            value sh {
                viewname {
                    Краткая форма
                }
                description {}
            }
        }

        feature NUMTYPE {
            viewname {
                Тип числительного
            }
            description {}
            value 1ended {
                viewname {
                    Заканчивающееся на "один"
                }
                description {
                    21; 541; 76551; "один"
                }
            }
            value small {
                viewname {
                    Оканчивающееся на "два", "три" или "четыре"
                }
                description {
                    2; 3; 4; 43; 104; 7862; "три"
                }
            }
            value large {
                viewname {
                    Оканчивающиеся на "пять", "шесть", "семь", "восемь", "девять", "ноль"
                }
                description {
                    5; 6; 26; 14; 807; 17; 110; 5119; 8488; "семь"
                }
            }
            value fractionDecimal {
                viewname {
                    Десятичная дробь
                }
                description {
                    0.75; 0,75; 3,084; 507.954
                }
            }
        }

        feature NUMORD {
            viewname {
                "Разрядность" числительного
            }
            description {}
            value tenhun {
                viewname {
                    Десятки
                }
                description {
                    10; 20; 30; 100; 800; 900; 5000
                }
            }
        }

        feature COLL {
            viewname {
                Собирательное числительное
            }
            description {}
            value coll {
                viewname {
                    Собирательное
                }
                description {
                    Двое, трое, четверо, ... десятеро
                }
            }
        }

        feature IMPERS {
            viewname {
                Безличность
            }
            description {}
            value impers {
                viewname {
                    Безличный глагол
                }
                description {}
            }
        }

        feature FREQ {
            viewname {
                Многократность
            }
            description {}
            value freq {
                viewname {
                    Многократный глагол
                }
                description {}
            }
        }

        feature REPR {
            viewname {
                Репрезентация глагола
            }
            description {}
            value inf {
                viewname {
                    Инфинитив
                }
                description {}
            }
            value fin {
                viewname {
                    Финитная репрезентация
                }
                description {}
            }
            value gern {
                viewname {
                    Герундий / деепричастие
                }
                description {}
            }
            value part {
                viewname {
                    Причастие
                }
                description {}
            }
        }

        feature MD {
            viewname {
                Наклонение
            }
            description {}
            value ind {
                viewname {
                    Индикатив (изъявительное)
                }
                description {}
            }
            value imp {
                viewname {
                    Императив (повелительное)
                }
                description {}
            }
        }

        feature TNS {
            viewname {
                Время
            }
            description {}
            value past {
                viewname {
                    Прошедшее
                }
                description {}
            }
            value pres {
                viewname {
                    Настоящее (презенс)
                }
                description {}
            }
            value fut {
                viewname {
                    Будущее простое
                }
                description {}
            }
        }

        feature PRS {
            viewname {
                Лицо
            }
            description {}
            value 3 {
                viewname {
                    Третье
                }
                description {}
            }
            value 2 {
                viewname {
                    Второе
                }
                description {}
            }
            value 1 {
                viewname {
                    Первое
                }
                description {}
            }
        }

        feature AUXIL {
            viewname {
                Вспомогательный глагол
            }
            description {}
            value auxil {
                viewname {}
                description {}
            }
        }

        feature VOX {
            viewname {
                Залог глагола
            }
            description {}
            value act {
                viewname {
                    Актив
                }
                description {}
            }
            value pass {
                viewname {
                    Пассив
                }
                description {}
            }
        }

        feature ASP {
            viewname {
                Аспект (вид) глагола
            }
            description {}
            value pf {
                viewname {
                    Перфектив (совершенный вид)
                }
                description {}
            }
            value ipf {
                viewname {
                    Имперфектив (несовершенный вид)
                }
                description {}
            }
            value pf_ipf {
                viewname {
                    Двувидовость (перфектив-имперфектив)
                }
                description {
                    Атаковать, жениться
                }
            }
        }

        feature TRANS {
            viewname {
                Переходность глагола
            }
            description {}
            value vt {
                viewname {
                    Переходный
                }
                description {}
            }
            value vi {
                viewname {
                    Непереходный
                }
                description {}
            }
        }

        feature GCAS {
            viewname {
                Падеж, которым управляет предлог
            }
            description {}
            value nom {
                viewname {
                    Номинатив (именительный)
                }
                description {}
            }
            value gen {
                viewname {
                    Генитив (родительный)
                }
                description {}
            }
            value dat {
                viewname {
                    Датив (дательный)
                }
                description {}
            }
            value acc {
                viewname {
                    Аккузатив (винительный)
                }
                description {}
            }
            value inst {
                viewname {
                    Инструменталис (творительный)
                }
                description {}
            }
            value prp {
                viewname {
                    Препозиционалис (предложный)
                }
                description {}
            }
            value prp2 {
                viewname {
                    Второй препозиционалис (второй предложный)
                }
                description {
                    "Работать в саду"
                }
            }
        }

        feature GPRON {
            viewname {
                Сочетаемость предлога с местоимением "он, она"
            }
            description {}
            value n {
                viewname {
                    Местоимение на "н-"
                }
                description {
                    "Для него"
                }
            }
            value s {
                viewname {}
                description {}
            }
            value j {
                viewname {
                    Местоимение на "j-"
                }
                description {
                    "Благодаря ей"
                }
            }
        }

        feature LR {
            viewname {}
            description {}

            value lr {
                viewname {}
                description {}
            }
        }
    }


    hierarchy standard {
        optional feature LR {
            value lr;
        }

        optional feature AUX {
            mark sys;
            value aux;
        }

        feature kind {
            mark sys;
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
            value complex {
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

        optional openset feature TYPE {}
        optional openset feature SYNTTYPE {}


        private openset feature base {
            mark lex;
        }

        openset feature WORDFORM {
            mark infl;
            mark sys;
        }

        optional openset feature freqOrthm {
            mark lex;
        }

        optional openset feature ZINDEX {
            mark lex;
        }

        optional feature ENDDOT {
            mark lex;
            value enddot;
        }

        openset  feature ACCPL {
            mark lex;
        }

        optional openset feature ACCSECPL {
            mark lex;
        }
        optional openset feature ACCYOPL {
            mark lex;
        }
        optional openset  feature DICTID {
            mark lex;
            mark sys;
        }
        openset  feature ID {
            mark lex;
            mark sys;
        }

        optional feature IRREG {
            mark lex;
            value irreg;
        }

        optional feature ANOM {
            mark sys;
            value anom;
        }


        feature POS {
            mark lex;
            value N {
                optional feature INVAR {
                    mark lex;
                    value invar;
                }

                optional feature LEXTYPE {
                    mark lex;
                    value propnoun {
                        optional feature PNT {
                            mark lex;
                            value fam {
                                feature GEND {
                                    mark infl;
                                    value m;
                                    value f;
                                    value mf;
                                }
                                feature ANIM {
                                    mark lex;
                                    value anim;
                                }
                            }
                            value fnam {
                                optional feature HPC {
                                    mark lex;
                                    value hpc;
                                }

                                optional feature OPGEND {
                                    mark lex;
                                    value opgend;
                                }

                                feature GEND {
                                    mark lex;
                                    value m;
                                    value f;
                                    value mf;
                                }
                                feature ANIM {
                                    mark lex;
                                    value anim;
                                }
                            }
                            value ptrn {
                                feature GEND {
                                    mark lex;
                                    value m;
                                    value f;
                                }
                                feature ANIM {
                                    mark lex;
                                    value anim;
                                }
                            }
                            value null {
                                feature GEND {
                                    mark lex;
                                    value m;
                                    value f;
                                    value n;
                                    value mf;
                                    value mn;
                                    value mfn;
                                    value plt;
                                }
                                feature ANIM {
                                    mark lex;
                                    value anim;
                                    value inan;
                                }
                            }
                        }
                    }
                    value null {
                        feature GEND {
                            mark lex;
                            value m;
                            value f;
                            value n;
                            value mf;
                            value mn;
                            value mfn;
                            value plt;
                        }

                        feature ANIM {
                            mark lex;
                            value anim;
                            value inan;
                        }
                    }
                }

                optional feature ADJI {
                    mark lex;
                    value adji;
                }

                optional feature PRN {
                    mark lex;
                    value prn {
                        optional feature IREL {
                            mark lex;
                            value irel;
                        }

                        optional feature ADPREP {
                            mark lex;
                            value adprep;
                        }
                    }
                }

                feature NMB {
                    mark infl;
                    value sg;
                    value pl;
                }

                feature CAS {
                    mark infl;
                    value nom;
                    value gen;
                    value gen2 {
                      mark invar_excl;
                    }
                    value dat;
                    value acc;
                    value inst;
                    value prp;
                    value prp2 {
                      mark invar_excl;
                    }
                }
            }

            value A {
                optional feature INVAR {
                    mark lex;
                    value invar;
                }

                optional feature PRN {
                    mark lex;
                    value prn {
                        optional feature IREL {
                            mark lex;
                            value irel;
                        }
                    }
                }

                optional feature ORDIN {
                    mark lex;
                    value ordin {
                        optional feature NOTATION {
                            mark lex;
                            value numerical;
                        }
                    }
                }

                feature DGR {
                    mark infl;
                    value comp {
                        mark invar_excl;
                    }
                    value null {
                        feature ATTR {
                            mark infl;
                            value sh {
                                mark invar_excl;
                                feature NMB {
                                    mark infl;
                                    value sg {
                                        feature GEND {
                                            mark infl;
                                            value m;
                                            value f;
                                            value n;
                                        }
                                    }
                                    value pl;
                                }
                            }
                            value null {
                                feature NMB {
                                    mark infl;
                                    value sg {
                                        feature GEND {
                                            mark infl;
                                            value m {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc {
                                                        feature ANIM {
                                                            mark infl;
                                                            value anim;
                                                            value inan;
                                                        }
                                                    }
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                            value f {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc;
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                            value n {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc;
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                        }
                                    }
                                    value pl {
                                        feature CAS {
                                            mark infl;
                                            value nom;
                                            value gen;
                                            value dat;
                                            value acc {
                                                feature ANIM {
                                                    mark infl;
                                                    value anim;
                                                    value inan;
                                                }
                                            }
                                            value inst;
                                            value prp;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            value NUM {
                optional feature COLL {
                    mark lex;
                    value coll {
                        feature CAS {
                            mark infl;
                            value nom;
                            value gen;
                            value dat;
                            value acc {
                                feature ANIM {
                                    mark infl;
                                    value anim;
                                    value inan;
                                }
                            }
                            value inst;
                            value prp;
                        }
                    }
                    value null {
                        optional feature INVAR {
                            mark lex;
                            value invar;
                        }

                        optional feature HASGEND {
                            mark lex;
                            value hasgend {
                                optional feature NOTATION {
                                    mark lex;
                                    value numerical;
                                }

                                 feature GEND {
                                     mark infl;
                                     value m;
                                     value f;
                                     value n;
                                 }
                                 feature CAS {
                                     mark infl;
                                     value nom;
                                     value gen;
                                     value dat;
                                     value acc {
                                         feature ANIM {
                                             mark infl;
                                             value anim;
                                             value inan;
                                         }
                                     }
                                     value inst;
                                     value prp;
                                 }
                                 feature NUMTYPE {
                                     mark lex;
                                     value 1ended;
                                     value small;
                                 }
                             }
                             value null {
                                optional feature PRN {
                                    mark lex;
                                    value prn {
                                        optional feature IREL {
                                            mark lex;
                                            value irel;
                                        }
                                        feature CAS {
                                            mark infl;
                                            value nom;
                                            value gen;
                                            value dat;
                                            value acc {
                                                feature ANIM {
                                                    mark infl;
                                                    value anim;
                                                    value inan;
                                                }
                                            }
                                            value inst;
                                            value prp;
                                        }
                                        feature NUMTYPE {
                                            mark lex;
                                            value large;
                                        }
                                    }
                                    value null {
                                        optional feature NOTATION {
                                            mark lex;
                                            value numerical;
                                        }

                                        optional feature NUMORD {
                                            mark lex;
                                            value tenhun;
                                        }
                                        feature NUMTYPE {
                                            mark lex;
                                            value 1ended {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc {
                                                        feature ANIM {
                                                            mark infl;
                                                            value anim;
                                                            value inan;
                                                        }
                                                    }
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                            value small {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc {
                                                        feature ANIM {
                                                            mark infl;
                                                            value anim;
                                                            value inan;
                                                        }
                                                    }
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                            value large {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc;
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                            value fractionDecimal {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc;
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            value V {
                optional feature AUXIL {
                    mark lex;
                    value auxil;
                }

                optional feature FREQ {
                    mark lex;
                    value freq;
                }

                feature IMPERS {
                    mark lex;
                    value impers {
                        feature REPR {
                            mark infl;
                            value inf;
                            value fin {
                                feature MD {
                                    mark infl;
                                    value ind {
                                        feature TNS {
                                            mark infl;
                                            value past {
                                                feature NMB {
                                                    mark infl;
                                                    value sg {
                                                        feature GEND {
                                                            mark infl;
                                                            value n;
                                                        }
                                                    }
                                                }
                                            }
                                            value pres {
                                                feature NMB {
                                                    mark infl;
                                                    value sg;
                                                }
                                                feature PRS {
                                                    mark infl;
                                                    value 3;
                                                }
                                            }
                                            value fut {
                                                feature NMB {
                                                    mark infl;
                                                    value sg;
                                                }
                                                feature PRS {
                                                    mark infl;
                                                    value 3;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            value gern {
                                feature TNS {
                                    mark infl;
                                    value past;
                                    value pres;
                                }
                            }

                            value part {
                                feature TNS {
                                    mark infl;
                                    value past;
                                    value pres;
                                }

                                feature ATTR {
                                    mark infl;
                                    value sh {
                                        feature NMB {
                                            mark infl;
                                            value sg {
                                                feature GEND {
                                                    mark infl;
                                                    value m;
                                                    value f;
                                                    value n;
                                                }
                                            }
                                            value pl;
                                        }
                                    }
                                    value null {
                                        feature NMB {
                                            mark infl;
                                            value sg {
                                                feature GEND {
                                                    mark infl;
                                                    value m {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc {
                                                                feature ANIM {
                                                                    mark infl;
                                                                    value anim;
                                                                    value inan;
                                                                }
                                                            }
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                    value f {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc;
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                    value n {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc;
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                }
                                            }
                                            value pl {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc {
                                                        feature ANIM {
                                                            mark infl;
                                                            value anim;
                                                            value inan;
                                                        }
                                                    }
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    value null {
                        optional feature VOX {
                            mark infl;
                            value pass;
                        }

                        feature REPR {
                            mark infl;
                            value inf;
                            value fin {
                                feature MD {
                                    mark infl;
                                    value ind {
                                        feature TNS {
                                            mark infl;
                                            value past {
                                                feature NMB {
                                                    mark infl;
                                                    value sg {
                                                        feature GEND {
                                                            mark infl;
                                                            value m;
                                                            value f;
                                                            value n;
                                                        }
                                                    }
                                                    value pl;
                                                }
                                            }
                                            value pres {
                                                feature NMB {
                                                    mark infl;
                                                    value sg;
                                                    value pl;
                                                }
                                                feature PRS {
                                                    mark infl;
                                                    value 1;
                                                    value 2;
                                                    value 3;
                                                }
                                            }
                                            value fut {
                                                feature NMB {
                                                    mark infl;
                                                    value sg;
                                                    value pl;
                                                }
                                                feature PRS {
                                                    mark infl;
                                                    value 1;
                                                    value 2;
                                                    value 3;
                                                }
                                            }
                                        }
                                    }
                                    value imp {
                                        feature PRS {
                                            mark infl;
                                            value 2;
                                        }
                                        feature NMB {
                                            mark infl;
                                            value sg;
                                            value pl;
                                        }
                                    }
                                }
                            }

                            value gern {
                                feature TNS {
                                    mark infl;
                                    value past;
                                    value pres;
                                }
                            }

                            value part {
                                feature TNS {
                                    mark infl;
                                    value past;
                                    value pres;
                                }

                                feature ATTR {
                                    mark infl;
                                    value sh {
                                        feature NMB {
                                            mark infl;
                                            value sg {
                                                feature GEND {
                                                    mark infl;
                                                    value m;
                                                    value f;
                                                    value n;
                                                }
                                            }
                                            value pl;
                                        }
                                    }
                                    value null {
                                        feature NMB {
                                            mark infl;
                                            value sg {
                                                feature GEND {
                                                    mark infl;
                                                    value m {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc {
                                                                feature ANIM {
                                                                    mark infl;
                                                                    value anim;
                                                                    value inan;
                                                                }
                                                            }
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                    value f {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc;
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                    value n {
                                                        feature CAS {
                                                            mark infl;
                                                            value nom;
                                                            value gen;
                                                            value dat;
                                                            value acc;
                                                            value inst;
                                                            value prp;
                                                        }
                                                    }
                                                }
                                            }
                                            value pl {
                                                feature CAS {
                                                    mark infl;
                                                    value nom;
                                                    value gen;
                                                    value dat;
                                                    value acc {
                                                        feature ANIM {
                                                            mark infl;
                                                            value anim;
                                                            value inan;
                                                        }
                                                    }
                                                    value inst;
                                                    value prp;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                feature ASP {
                    mark lex;
                    value pf;
                    value ipf;
                    value pf_ipf;
                }

                optional feature TRANS {
                    mark lex;
                    value vt;
                    value vi;
                }
            }


            value PREP {
                optional feature Klitik {
                    mark lex;
                    value klitik;
                }

                feature GCAS {
                    mark lex;
                    value nom;
                    value gen;
                    value dat;
                    value acc;
                    value inst;
                    value prp;
                    value prp2;
                }

                feature GPRON {
                    mark lex;
                    value n;
                    value s;
                    value j;
                }

            }

            value ADV {
                optional feature PRN {
                    mark lex;
                    value prn {
                        optional feature IREL {
                            mark lex;
                            value irel;
                        }
                    }
                }
            }

            value PRED;
            value PAR;
            value CONJ {
                optional feature Klitik {
                    mark lex;
                    value klitik;
                }
            }
            value INTJ {
                optional feature Klitik {
                    mark lex;
                    value klitik;
                }
            }
            value PCL {
                optional feature Klitik {
                    mark lex;
                    value klitik;
                }
            }
        }
    }
}

type Sentence {
    introduction {}
    hierarchy standard {}
}

type Syntax extends Gramm {
    introduction {
        feature PGROUP {
            viewname {}
            description {}

            value clause {
                viewname {}
                description {}
            }
            value gerg {
                viewname {}
                description {}
            }
            value partg {
                viewname {}
                description {}
            }
            value infg {
                viewname {}
                description {}
            }
        }

        feature PHRASE {
            viewname {}
            description {}

            value prepg {
                viewname {}
                description {}
            }

            value conjN {
                viewname {}
                description {}
            }

            value conjg {
                viewname {}
                description {}
            }
        }

        feature CONJ {
            viewname {}
            description {}

            value conjelem_N {
                viewname {}
                description {}
            }

            value conjelem_prepg {
                viewname {}
                description {}
            }

            value conjgroup {
                viewname {}
                description {}
            }
        }
    }

    hierarchy standard extends Gramm.standard {
        optional feature PGROUP {
            value clause;
            value gerg;
            value partg;
            value infg;
        }
        optional feature PHRASE {
            value prepg;
            value conjN;
            value conjg;
        }

        optional feature CONJ {
            value conjelem_N;
            value conjelem_prepg;
            value conjgroup;
        }
    }
}


type EtapMorph {
    introduction {
        feature lemma {
            viewname {}
            description {}
        }

        feature pos {
            viewname {}
            description {}
        }
    }
    hierarchy standard {}
}
