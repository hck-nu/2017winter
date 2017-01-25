/*
Steps!!!
1. Make demo with if(mousepressed){fill(0);}else{fill(255);}ellipse(mouseX,mouseY,50,50);
-> Make sure to explain setup() and draw()
2. Explain raindrops
3. Make raindrop class
4. make raindrop list
5. Finish out the draw
6. Update background with fill
7. Make the ground
8. Separate snow into its own class
*/

var scene;
var SCENE;

function setup() {
    createCanvas(window.innerWidth, window.innerHeight);
    SCENE = "snow";

    switch (SCENE) {
        case "snow":
            scene = new snowScene();
            break;
        default:
            return;
    }
}

function draw() {
    if(mouseIsPressed){
        scene.raindrops = [];
    }
    scene.display();
}

function snowScene() {
    this.raindrops = [];

    this.display = function() {
        background(0, 128);
        noStroke();
        fill(255);
        rect(0, height-100, width, height)
        fill(255, 128);
        // Use millis but switch to frameCount to show it's better
        if((frameCount % 10) == 0){
            this.raindrops.push(new raindrop());
        }
        for (var i=this.raindrops.length-1; i >= 0; i-=1){
            if(this.raindrops[i].y >= window.innerWidth){
                this.raindrops.splice(i, 1);
            } else {
                this.raindrops[i].move();
                this.raindrops[i].display();
            }
        }
    }
}

function raindrop() {
    this.x = random(window.innerWidth);
    this.y = 0;
    this.speed = 1;
    this.size = 6;

    this.move = function() {
        this.y += this.speed;
    }

    this.display = function() {
        ellipse(this.x, this.y, this.size, this.size);
    }
}
