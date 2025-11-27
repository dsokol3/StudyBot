# AI ChatBot - Vue + Java

A modern AI chatbot application with Vue 3 frontend and Java Spring Boot backend.

## 🚀 Tech Stack

### Frontend
- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type-safe development
- **Vite** - Fast build tool
- **TailwindCSS** - Utility-first CSS framework
- **shadcn-vue** - Beautiful UI components
- **Axios** - HTTP client for API communication
- **Lucide Icons** - Beautiful icon library

### Backend
- **Java 21** - LTS version
- **Spring Boot 3.2** - Application framework
- **Maven** - Build tool
- **OpenAI API** - AI chat capabilities

## 📁 Project Structure

```
ChatBot/
├── frontend/          # Vue 3 frontend
│   ├── src/
│   │   ├── App.vue           # Main chat component
│   │   ├── services/api.ts   # API client
│   │   ├── lib/utils.ts      # Utility functions
│   │   └── style.css         # Global styles
│   ├── package.json
│   └── vite.config.ts
├── src/main/java/    # Java backend
│   └── com/chatbot/
│       ├── controller/       # REST controllers
│       ├── service/          # Business logic
│       └── model/            # Data models
└── pom.xml
```

## 🛠️ Setup & Installation

### Prerequisites
- Node.js 18+ and npm
- Java 21 JDK
- Maven 3.6+

### Backend Setup

1. Navigate to project root:
```bash
cd ChatBot
```

2. Configure OpenAI API key in `src/main/resources/application.properties`:
```properties
openai.api.key=your-actual-api-key-here
```

3. Build and run the backend:
```bash
mvn clean install
mvn spring-boot:run
```

Backend will run on `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start development server:
```bash
npm run dev
```

Frontend will run on `http://localhost:5173`

## 🎨 Using Figma for UI Design

### Getting Started with Figma

1. **Create a Figma Account**
   - Visit [figma.com](https://figma.com)
   - Sign up for free
   - Download Figma desktop app (optional)

2. **Design Your ChatBot UI**
   - Create a new design file
   - Use Figma's design tools to mockup your chatbot interface
   - Design components: chat bubbles, input fields, buttons, headers
   - Set up color schemes and typography

3. **TailwindCSS Integration**
   - Use Figma plugins like "Tailwind CSS" or "Design Tokens"
   - Export spacing, colors, and sizing that match Tailwind utilities
   - Document design tokens in Figma for consistency

4. **Recommended Figma Plugins**
   - **Tailwind CSS** - Convert Figma designs to Tailwind classes
   - **Iconify** - Access to thousands of icons including Lucide
   - **Design Lint** - Check design consistency
   - **Content Reel** - Generate realistic chat messages for mockups

5. **Design System Setup**
   - Create a component library in Figma
   - Define color variables matching your `style.css` CSS variables
   - Create reusable components (buttons, inputs, cards)
   - Document spacing and sizing scales

6. **Export Assets**
   - Export icons as SVG
   - Use CSS code from Figma inspect panel
   - Copy spacing and color values to your Tailwind config

### Design Tips for ChatBot UI

1. **Chat Messages**
   - User messages: Right-aligned, primary color
   - AI messages: Left-aligned, muted background
   - Clear visual distinction between senders

2. **Input Area**
   - Fixed at bottom
   - Clear send button
   - Loading state indicator
   - Disabled state when processing

3. **Color Scheme**
   - Use the existing CSS variables in `style.css`
   - Light and dark mode support
   - Accessible contrast ratios (WCAG AA)

4. **Responsive Design**
   - Mobile-first approach
   - Test on different screen sizes
   - Consider touch targets (min 44x44px)

### Figma to Code Workflow

1. Design in Figma → Review with team
2. Export design specs and assets
3. Convert Figma designs to Vue components
4. Use shadcn-vue components that match your design
5. Customize Tailwind config based on Figma tokens

## 🎨 Customizing the UI

### Colors
Edit the CSS variables in `frontend/src/style.css` to match your Figma design:

```css
:root {
  --primary: 221.2 83.2% 53.3%;
  --secondary: 210 40% 96.1%;
  /* Add more custom colors */
}
```

### Tailwind Configuration
Update `frontend/tailwind.config.js` for custom theme values:

```js
theme: {
  extend: {
    colors: {
      // Your custom colors from Figma
    },
  },
}
```

## 📡 API Endpoints

- `POST /api/chat/message` - Send a message
- `DELETE /api/chat/conversation/{id}` - Clear conversation
- `GET /api/chat/health` - Health check

## 🧩 Adding shadcn-vue Components

To add more UI components from shadcn-vue:

```bash
cd frontend
npx shadcn-vue@latest add button
npx shadcn-vue@latest add card
npx shadcn-vue@latest add input
```

## 🔧 Development

### Frontend Development
```bash
cd frontend
npm run dev        # Start dev server
npm run build      # Build for production
npm run preview    # Preview production build
```

### Backend Development
```bash
mvn spring-boot:run              # Run application
mvn test                         # Run tests
mvn clean package                # Build JAR
```

## 🚀 Production Build

### Frontend
```bash
cd frontend
npm run build
```

Build output will be in `frontend/dist/`

### Backend
```bash
mvn clean package -DskipTests
```

JAR file will be in `target/chatbot-1.0-SNAPSHOT.jar`

Run with:
```bash
java -jar target/chatbot-1.0-SNAPSHOT.jar
```

## 🎯 Features

- ✅ Real-time chat interface
- ✅ Multi-threaded message processing
- ✅ Conversation history management
- ✅ Modern UI with TailwindCSS
- ✅ Type-safe TypeScript frontend
- ✅ RESTful API backend
- ✅ OpenAI integration
- ✅ Responsive design
- ✅ Dark/Light mode support

## 📝 Environment Variables

### Backend (`application.properties`)
```properties
server.port=8080
openai.api.key=your-api-key-here
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

### Frontend
API calls are proxied through Vite dev server. In production, update `src/services/api.ts`:

```typescript
const API_BASE_URL = import.meta.env.PROD 
  ? 'https://your-production-api.com/api' 
  : 'http://localhost:8080/api'
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

MIT License

## 🆘 Troubleshooting

### Port Already in Use
- Backend: Change port in `application.properties`
- Frontend: Change port in `vite.config.ts`

### CORS Issues
CORS is configured in `WebConfig.java`. Update allowed origins as needed.

### API Connection Errors
- Ensure backend is running on port 8080
- Check Vite proxy configuration
- Verify firewall settings

---

**Happy Coding! 🎉**
