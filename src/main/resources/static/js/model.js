function GetQueryString(name) {
    var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if(r!=null)
        return  decodeURI(r[2]);
    return null;
}
function del(obj){
    var thisobj=$(obj);         //js对象转jquery对象
    var id= thisobj.attr("id");
    var num=id.substr(11);
    $("#field_seed_li"+num).remove();
}
function del_concept(obj){
    var thisobj=$(obj);
    var id=thisobj.attr("id");
    var num=id.substr(14);
    $("#add_concept"+num).remove();
}
function del_con_seed(obj) {
    var thisobj=$(obj);
    var strs=thisobj.attr("id").split("_");
    var con=strs[2];
    var seed=strs[3];
    $("#seed_"+con+"_"+seed).remove();
}
function add_seed(obj){
    var thisobj=$(obj);
    var num=thisobj.attr("id").substr(9);
    $("#seed"+num).append($('<div id="seed_'+(concept-1)+'_'+(++record[concept-1])+'">\n' +
        '                        <h3 class="field_name">种子词</h3>\n'+
        '                        <input type="text" name="concept'+(concept-1)+'_seed'+record[concept-1]+'" value=""/>\n' +
        '                        <button type="button" id="del_seed_'+(concept-1)+'_'+record[concept-1]+'" onclick="del_con_seed(this)">删除</button>\n' +
        '                    </div>'));
}

function fill_form(field){
    $.get("model.html?field="+field,function (data,statu) {

    })
}

var field_seed=1;
var concept=1;
var record=new Array();     //记录每一个概念词下种子词的个数
$(document).ready(function(){
    //如果参数不为空则为修改原有信息，填充表单
    if(GetQueryString("field")!=null){
        fill_form(GetQueryString("field"));
    }
    //添加领域种子词
    $("#add_seed1").click(function(){
        $("#field_seed").append($('<li id="field_seed_li'+field_seed+'"><input type="text" name="field_seed_input'+field_seed+'" value=""/><button type="button" onclick="del(this)" id="delete_seed'+field_seed+'">删除</button></li>'));
        field_seed++;
    });
    //添加概念
    $("#add_concept").click(function () {
        record[concept]=1;
        $("#concept_container").append($('<div id="add_concept'+concept+'">\n' +
            '                        <div>\n' +
            '                            <h3 class="field_name">概念词</h3>\n' +
            '                            <input type="text" name="concept_name_input'+concept+'" value=""/>\n' +
            '                            <button type="button" id="delete_concept'+concept+'" onclick="del_concept(this)">删除</button>\n' +
            '                        </div>\n' +
            '                        <div id="seed'+concept+'">\n' +
            '                            <div id="seed_'+concept+'_1">\n' +
            '                                <h3 class="field_name">种子词</h3>\n' +
            '                                <input type="text" name="concept'+concept+'_seed1" value=""/><span class="star">*</span>\n' +
            '                            </div>\n' +
            '                        </div>\n' +
            '                        <button type="button" id="add_seed_'+concept+'" onclick="add_seed(this)">添加种子词</button>\n' +
            '                    </div>'));
        concept++;
    });
});