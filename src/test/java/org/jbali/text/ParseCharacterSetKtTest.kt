package org.jbali.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseCharacterSetKtTest {

    private fun t(expect: String, input: String) {
        val parse = parseCharacterSet(input)
        println(parse)
        println(parse.toConcatString())
        assertEquals(expect, parse.toConcatString())

        val expectSet = expect.toSet()
        assertEquals(expectSet, parse)
//        assertEquals(expectSet.toString(), parse.toString())
    }

    @Test fun test() {

        t("", "")
        t("a", "a")

        t("-ABC_abc", "abcABC_-")
        t("-ABC_abc", "ABCabc_-")
        t("-ABC_abc", "a-cA-C_-")
        t("-ABC_abc", "-a-cA-C_")
        t("-ABC_abc", "-A-Ca-c_")
        t("-ABC_abc", "-A-Ca-c_")

        t("012ABC", "A-C0-2")
        t("-ABC", "-A-C")

        t(" -0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz", "A-Za-z0-9_ -")

        // almost all "latin" chars
        t(
                " -0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyzÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ"+
                        "ßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķ"+
                        "ĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐ"+
                        "ƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤ"+
                        "ǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹ"+
                        "ȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏḂḃḄḅḆḇḈḉḊḋḌḍḎḏḐḑḒḓḔḕḖḗḘḙḚḛḜḝḞḟḠḡḢḣḤḥḦḧḨḩḪḫḬḭḮḯḰḱḲḳḴḵḶḷḸḹḺḻḼḽḾḿṀṁṂṃṄṅṆ"+
                        "ṇṈṉṊṋṌṍṎṏṐṑṒṓṔṕṖṗṘṙṚṛṜṝṞṟṠṡṢṣṤṥṦṧṨṩṪṫṬṭṮṯṰṱṲṳṴṵṶṷṸṹṺṻṼṽṾṿẀẁẂẃẄẅẆẇẈẉẊẋẌẍẎẏẐẑẒẓẔẕẖẗẘẙẚẛẜẝẞẟẠ"+
                        "ạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳ",

                "0-9A-Za-zÀ-ÖØ-öø-ÿĀ-ſƀ-ɏḂ-ỳ_ -"
        )

        assertFailsWith<IllegalArgumentException> { parseCharacterSet("A-C--") }
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("C-A") }
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("A-Za-z0-9_- ") }

        // overlap
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("A-KF-Z") }
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("AA") }
    }

}