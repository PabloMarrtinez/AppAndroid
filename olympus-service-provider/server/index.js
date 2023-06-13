var express = require('express');
const exec = require('child_process').exec;
const fetch = require("node-fetch");
const path = require('path')
var fs = require('fs')
var https = require('https')
const app = express()
var certOptions = {
  key: fs.readFileSync(path.resolve('keys/server.key')),
  cert: fs.readFileSync(path.resolve('keys/server.cert'))
}
var port = 3000;
var portHttps = 3001;
var endpointList = ["https://localhost:9933","https://localhost:9934","https://localhost:9935"];
var truststorePath= "server/ol-lib/truststore.jks"
var truststorePassword= "OLYMPUS"
var ids=new Set();
var verifier_port = 3030;
var fourteenDaysOffset=1209600000;
const router = express.Router()
const bodyParser = require('body-parser');




spawnNewVerifier();
setTimeout(() => {
	setupverifier(endpointList, verifier_port).then(response => {
		if(response.status < 400) {
			console.log("Verifier setup at port "+verifier_port)
		}
	});    
}, 5000);

var basePolicy = {
	"predicates": [{
		"attributeName": "https://olympus-example-use-case.org/attributes/FamilyName",
		"operation": "REVEAL"
	}, {
		"attributeName": "https://olympus-example-use-case.org/attributes/VaccinationDate",
		"operation": "LESSTHANOREQUAL",
		"value": {
			"attr": 1622505600000,
			"type": "DATE"
		}
	}],
	"policyId": "RandomId"}

router.get('/getpolicy', function(req, res, next) {
	var result=generatePolicy();
  	res.status(200).send(result);
});

router.post('/present', function(req, res, next) {
	if(!checkPolicyId(req.body.policyId)){
        console.log("Policy id check failed");
		res.status(200).send(false);
    }
	else{
		var pol=reGeneratePolicy(req.body.policyId);
		var bodyRequest=new Object();
		bodyRequest.token=req.body.token;
		bodyRequest.policy=pol;
		verifytoken(bodyRequest, verifier_port).then(response => {
			res.status(200).send(JSON.stringify(response)); 
		});
	}
});

router.get('*', (_, res) => {
    return res.sendFile(path.resolve('build', 'index.html'))
  })


function spawnNewVerifier() {
    exec('java -jar server/ol-lib/verifier.jar' + " " +  verifier_port+ " " +  truststorePath+ " " +  truststorePassword, function(err, stdout, stderr) {
        if(stdout) {
            console.log(stdout);
        }
        if(stderr) {
            console.log(stderr);
        }
        if (err) { 
            console.log(err);
        }
    });
}

function checkPolicyId(id){
	if(ids.has(id)){
		ids.delete(id);
		return true;
	}
	return false;
}

function generatePolicy() {
	var res=JSON.parse(JSON.stringify(basePolicy));
	var id=getRandomInt(1,100000).toString();
	ids.add(id);
	res.policyId=id;
	var ms=Date.now();
	var msPerDay = 86400 * 1000;
    var beginning = ms - (ms % msPerDay);
    beginning += ((new Date).getTimezoneOffset() * 60 * 1000);	
	res.predicates[1].value.attr = beginning-fourteenDaysOffset;
	return res;
}

function reGeneratePolicy(policyId) {
	var res=JSON.parse(JSON.stringify(basePolicy));
	res.policyId=policyId;
	var ms=Date.now();
	var msPerDay = 86400 * 1000;
    var beginning = ms - (ms % msPerDay);
    beginning += ((new Date).getTimezoneOffset() * 60 * 1000);	
	res.predicates[1].value.attr = beginning-fourteenDaysOffset;
	return res;
}

async function setupverifier(endpoints, p) {
    return new Promise(function(resolve, reject) {
        var data = JSON.stringify({urls: endpoints});
        (async () => {
            const rawResponse = await fetch('http://localhost:'+ p + '/verifier/setup', {
              method: 'POST',
              headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
              },
              body: data
            });
            resolve(await rawResponse);
          })();
    });
}

function getRandomInt(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

async function verifytoken(request, p) {
    return new Promise(function(resolve, reject) {
        //console.log("REQUEST "+JSON.stringify(request));
        fetch('http://localhost:'+ p + '/verifier/verify', {
            method: 'post',
            headers: {
              'Accept': 'application/json, text/plain, */*',
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
          }).then(res => {
              resolve(res.json());
          }).catch(reject=>{
			console.log(reject)
		  });
    });
}

app.use(bodyParser.json());
app.use(express.static(path.join('build')));
app.use("/", router)
var server = https.createServer(certOptions, app).listen(portHttps)
app.listen(port, () => console.log(`Running on port ${port}.`))
