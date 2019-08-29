function GetQueryString(name) {
    var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if(r!=null)
        return  decodeURI(r[2]);
    return null;
}

function  form_submit() {
    $("form").submit(function (e) {
        getCorpus();
    })
}

function getCorpus(){
    $("#corpus_table td").each(function () {
        $(this).remove();
    });
    $.get('row_corpus?field='+GetQueryString("field"),function (data, status) {
        var i;
        for(i in data["corpus"]){
            $("#corpus_table").append($('<tr>\n' +
                '                           <td>'+data["corpus"][i]["fname"]+'</td>\n' +
                '                           <td>'+data["corpus"][i]["fsize"]+'</td>\n' +
                '                           <td>'+data["corpus"][i]["time"]+'</td>\n' +
                '                       </tr>'));
        }
    });
}
$(function () {
    getCorpus();
    $("#login").click(function(){
        $(location).attr("href","login.html");
    });
    $("#register").click(function(){
        $(location).attr("href","login.html");
    });
});